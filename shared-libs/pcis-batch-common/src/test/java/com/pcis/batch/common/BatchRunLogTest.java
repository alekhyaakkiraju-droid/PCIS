package com.pcis.batch.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

class BatchRunLogWriterTest {

  @Test
  void write_insertsRunLogRow() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    BatchRunLogWriter writer = new BatchRunLogWriter(jdbcTemplate);
    Instant end = Instant.parse("2026-08-11T12:00:00Z");
    Instant start = Instant.parse("2026-08-11T11:00:00Z");

    writer.write(
        new BatchRunLogEntry(
            "AUD002B",
            LocalDate.of(2026, 8, 11),
            2,
            2,
            0,
            null,
            start,
            end,
            "AUD002B"));

    verify(jdbcTemplate)
        .update(
            eq(
                """
                INSERT INTO RPT_RUN_LOG_T (
                    PGM_NAME,
                    RUN_DATE,
                    REC_SELECTED,
                    REC_UPDATED,
                    REC_ERRORS,
                    REC_DELINQUENT,
                    START_TIMESTAMP,
                    END_TIMESTAMP,
                    CRT_USER,
                    CRT_TIMESTAMP)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """),
            eq("AUD002B"),
            any(),
            eq(2),
            eq(2),
            eq(0),
            eq(null),
            any(),
            any(),
            eq("AUD002B"),
            any());
  }
}

class BatchRunLogConfigServiceTest {

  @Test
  void usesTunableWhenPresent() {
    TunableResolver resolver = mock(TunableResolver.class);
    ObjectProvider<TunableResolver> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(resolver);
    when(resolver.getBoolean(TunableKey.BATCH_RUN_LOG_ENABLED)).thenReturn(false);

    BatchRunLogConfigService service =
        new BatchRunLogConfigService(provider, new BatchRunLogProperties());

    assertThat(service.isRunLogEnabled()).isFalse();
  }

  @Test
  void fallsBackToPropertiesWhenTunableMissing() {
    TunableResolver resolver = mock(TunableResolver.class);
    ObjectProvider<TunableResolver> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(resolver);
    when(resolver.getBoolean(TunableKey.BATCH_RUN_LOG_ENABLED))
        .thenThrow(new TunableNotFoundException(TunableKey.BATCH_RUN_LOG_ENABLED.key()));

    BatchRunLogProperties properties = new BatchRunLogProperties();
    properties.setEnabled(true);
    BatchRunLogConfigService service = new BatchRunLogConfigService(provider, properties);

    assertThat(service.isRunLogEnabled()).isTrue();
  }

  @Test
  void usesPropertiesWhenResolverUnavailable() {
    ObjectProvider<TunableResolver> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);

    BatchRunLogProperties properties = new BatchRunLogProperties();
    properties.setEnabled(false);
    BatchRunLogConfigService service = new BatchRunLogConfigService(provider, properties);

    assertThat(service.isRunLogEnabled()).isFalse();
  }
}

class BatchRunLogTaskletTest {

  @Test
  void skipsWriteWhenRunLogDisabled() throws Exception {
    BatchRunLogWriter writer = mock(BatchRunLogWriter.class);
    BatchRunLogConfigService configService = mock(BatchRunLogConfigService.class);
    when(configService.isRunLogEnabled()).thenReturn(false);

    BatchRunLogTasklet tasklet =
        new BatchRunLogTasklet(
            writer,
            configService,
            "AUD002B",
            ctx -> BatchRunLogCounters.of(1, 1, 0),
            () -> LocalDate.now(ZoneOffset.UTC));

    RepeatStatus status = tasklet.execute(mock(StepContribution.class), mock(ChunkContext.class));

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(writer, never()).write(any());
  }

  @Test
  void writesRunLogWhenEnabled() throws Exception {
    BatchRunLogWriter writer = mock(BatchRunLogWriter.class);
    BatchRunLogConfigService configService = mock(BatchRunLogConfigService.class);
    when(configService.isRunLogEnabled()).thenReturn(true);

    JobExecution jobExecution = mock(JobExecution.class);
    JobInstance jobInstance = mock(JobInstance.class);
    StepExecution stepExecution = mock(StepExecution.class);
    StepContext stepContext = mock(StepContext.class);
    ChunkContext chunkContext = mock(ChunkContext.class);
    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(jobExecution.getStartTime()).thenReturn(null);
    when(jobExecution.getStepExecutions()).thenReturn(java.util.Set.of(stepExecution));
    when(stepExecution.getFailureExceptions()).thenReturn(java.util.List.of());

    BatchRunLogTasklet tasklet =
        new BatchRunLogTasklet(
            writer,
            configService,
            "AUD002B",
            ctx -> BatchRunLogCounters.of(3, 3, 0),
            () -> LocalDate.of(2026, 8, 11));

    tasklet.execute(mock(StepContribution.class), chunkContext);

    verify(writer).write(any(BatchRunLogEntry.class));
  }
}

class BatchJobRunLogSupportTest {

  @Test
  void countStepErrors_sumsFailureExceptions() {
    StepExecution failingStep = mock(StepExecution.class);
    StepExecution cleanStep = mock(StepExecution.class);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution currentStep = mock(StepExecution.class);
    StepContext stepContext = mock(StepContext.class);
    ChunkContext chunkContext = mock(ChunkContext.class);

    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(currentStep);
    when(currentStep.getJobExecution()).thenReturn(jobExecution);
    when(jobExecution.getStepExecutions()).thenReturn(java.util.Set.of(failingStep, cleanStep));
    when(failingStep.getFailureExceptions())
        .thenReturn(java.util.List.of(new IllegalStateException("boom")));
    when(cleanStep.getFailureExceptions()).thenReturn(java.util.List.of());

    assertThat(BatchJobRunLogSupport.countStepErrors(chunkContext)).isEqualTo(1);
  }
}
