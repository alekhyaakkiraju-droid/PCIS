package com.pcis.batch.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

class BatchCommonTest {

  @Test
  void batchExitCodeJobListener_registersResolvedExitCode() {
    BatchCommonAutoConfiguration.BatchProcessExitCode exitCode =
        new BatchCommonAutoConfiguration.BatchProcessExitCode();
    BatchExitCodeJobListener listener = new BatchExitCodeJobListener(exitCode);

    JobExecution execution = mock(JobExecution.class);
    JobInstance instance = mock(JobInstance.class);
    when(execution.getJobInstance()).thenReturn(instance);
    when(instance.getJobName()).thenReturn("auditArchiveJob");
    when(execution.getStatus()).thenReturn(BatchStatus.FAILED);
    ExecutionContext context = new ExecutionContext();
    context.putInt(BatchJobExitCodeListener.EXIT_CODE_CONTEXT_KEY, 2);
    when(execution.getExecutionContext()).thenReturn(context);

    listener.afterJob(execution);

    assertThat(exitCode.getExitCode()).isEqualTo(2);
  }

  @Test
  void outboxEventWriter_insertsPendingRow() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    OutboxEventWriter writer =
        new OutboxEventWriter(jdbcTemplate, new ObjectMapper(), "AUD002B");
    UUID key = UUID.randomUUID();

    writer.write(
        "audit-archive",
        "chunk-1",
        "ChunkArchived",
        Map.of("archivedCount", 2),
        key);

    verify(jdbcTemplate)
        .update(
            eq(
                """
                INSERT INTO outbox_events (
                    AGGREGATE_TYPE,
                    AGGREGATE_ID,
                    EVENT_TYPE,
                    PAYLOAD,
                    IDEMPOTENCY_KEY,
                    STATUS,
                    ATTEMPT_COUNT,
                    NEXT_ATTEMPT_AT,
                    CRT_USER,
                    CRT_TIMESTAMP)
                VALUES (?, ?, ?, ?::jsonb, ?, 'PENDING', 0, ?, ?, ?)
                """),
            eq("audit-archive"),
            eq("chunk-1"),
            eq("ChunkArchived"),
            any(String.class),
            eq(key),
            any(),
            eq("AUD002B"),
            any());
  }
}
