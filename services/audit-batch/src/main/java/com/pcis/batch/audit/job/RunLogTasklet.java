package com.pcis.batch.audit.job;

import com.pcis.batch.audit.config.AuditArchiveProperties;
import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

public class RunLogTasklet implements Tasklet {

  private static final String INSERT_RUN_LOG =
      """
      INSERT INTO RPT_RUN_LOG_T (
          PGM_NAME,
          RUN_DATE,
          REC_SELECTED,
          REC_UPDATED,
          REC_ERRORS,
          START_TIMESTAMP,
          END_TIMESTAMP,
          CRT_USER,
          CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final AuditArchiveProperties properties;

  public RunLogTasklet(JdbcTemplate jdbcTemplate, AuditArchiveProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ExecutionContext jobContext =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
    long archived = jobContext.getLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 0L);
    int errors =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getStepExecutions().stream()
            .mapToInt(step -> step.getFailureExceptions().size())
            .sum();

    Instant end = Instant.now();
    Instant start =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getStartTime() != null
            ? chunkContext
                .getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getStartTime()
                .atZone(java.time.ZoneOffset.UTC)
                .toInstant()
            : end;

    jdbcTemplate.update(
        INSERT_RUN_LOG,
        properties.getProgramName(),
        Date.valueOf(end.atZone(java.time.ZoneOffset.UTC).toLocalDate()),
        (int) archived,
        (int) archived,
        errors,
        Timestamp.from(start),
        Timestamp.from(end),
        properties.getProgramName(),
        Timestamp.from(end));

    return RepeatStatus.FINISHED;
  }
}
