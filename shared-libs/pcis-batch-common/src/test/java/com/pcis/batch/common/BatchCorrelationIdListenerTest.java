package com.pcis.batch.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

class BatchCorrelationIdListenerTest {

  private final BatchCorrelationIdListener listener = new BatchCorrelationIdListener();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void beforeJob_usesCorrelationIdJobParameter() {
    JobExecution execution =
        jobExecution(new JobParametersBuilder().addString("correlationId", "corr-batch-1").toJobParameters());

    listener.beforeJob(execution);

    assertThat(MDC.get("correlationId")).isEqualTo("corr-batch-1");
    assertThat(MDC.get("jobId")).isEqualTo("billingJob");
    assertThat(MDC.get("runId")).isEqualTo("42");
  }

  @Test
  void beforeJob_generatesUuidWhenParameterMissing() {
    listener.beforeJob(jobExecution(new JobParametersBuilder().toJobParameters()));

    assertThat(MDC.get("correlationId")).isNotBlank();
  }

  @Test
  void beforeJob_rejectsUnsafeCorrelationId() {
    listener.beforeJob(
        jobExecution(
            new JobParametersBuilder().addString("correlationId", "evil\r\nInjected").toJobParameters()));

    assertThat(MDC.get("correlationId")).isNotBlank().isNotEqualTo("evil\r\nInjected");
  }

  @Test
  void afterJob_clearsMdc() {
    JobExecution execution = jobExecution(new JobParametersBuilder().addString("correlationId", "corr-99").toJobParameters());
    listener.beforeJob(execution);

    listener.afterJob(execution);

    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  void resolveCorrelationId_prefersSafeJobParameter() {
    JobExecution execution =
        jobExecution(new JobParametersBuilder().addString("correlationId", "job-param-id").toJobParameters());

    assertThat(BatchCorrelationIdListener.resolveCorrelationId(execution)).isEqualTo("job-param-id");
  }

  private static JobExecution jobExecution(JobParameters parameters) {
    JobExecution execution = mock(JobExecution.class);
    JobInstance instance = mock(JobInstance.class);
    when(execution.getJobInstance()).thenReturn(instance);
    when(instance.getJobName()).thenReturn("billingJob");
    when(execution.getId()).thenReturn(42L);
    when(execution.getJobParameters()).thenReturn(parameters);
    return execution;
  }
}
