package com.pcis.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

class BatchJobExitCodeListenerTest {

  @Test
  void afterJob_recordsResolvedExitCode() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BatchJobExitCodeMetrics metrics = new BatchJobExitCodeMetrics(registry);
    BatchJobExitCodeListener listener = new BatchJobExitCodeListener(metrics);

    JobExecution execution = mock(JobExecution.class);
    JobInstance instance = mock(JobInstance.class);
    whenJob(execution, instance, "billing-installment-job", BatchStatus.COMPLETED, null);

    listener.afterJob(execution);

    assertThat(metrics.exitCodeFor("billing-installment-job")).isEqualTo(0.0);
  }

  @Test
  void afterJob_usesExecutionContextExitCodeWhenPresent() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BatchJobExitCodeMetrics metrics = new BatchJobExitCodeMetrics(registry);
    BatchJobExitCodeListener listener = new BatchJobExitCodeListener(metrics);

    JobExecution execution = mock(JobExecution.class);
    JobInstance instance = mock(JobInstance.class);
    whenJob(execution, instance, "audit-archive-job", BatchStatus.FAILED, 2);

    listener.afterJob(execution);

    assertThat(metrics.exitCodeFor("audit-archive-job")).isEqualTo(2.0);
  }

  @Test
  void resolveExitCode_mapsBatchStatusToContract() {
    JobExecution completed = mock(JobExecution.class);
    when(completed.getExecutionContext()).thenReturn(new org.springframework.batch.item.ExecutionContext());
    when(completed.getStatus()).thenReturn(BatchStatus.COMPLETED);
    assertThat(BatchJobExitCodeListener.resolveExitCode(completed)).isZero();

    JobExecution failed = mock(JobExecution.class);
    when(failed.getExecutionContext()).thenReturn(new org.springframework.batch.item.ExecutionContext());
    when(failed.getStatus()).thenReturn(BatchStatus.FAILED);
    assertThat(BatchJobExitCodeListener.resolveExitCode(failed)).isEqualTo(1);
  }

  private static void whenJob(
      JobExecution execution,
      JobInstance instance,
      String jobName,
      BatchStatus status,
      Integer contextExitCode) {
    when(execution.getJobInstance()).thenReturn(instance);
    when(instance.getJobName()).thenReturn(jobName);
    when(execution.getStatus()).thenReturn(status);
    org.springframework.batch.item.ExecutionContext context =
        new org.springframework.batch.item.ExecutionContext();
    if (contextExitCode != null) {
      context.putInt(BatchJobExitCodeListener.EXIT_CODE_CONTEXT_KEY, contextExitCode);
    }
    when(execution.getExecutionContext()).thenReturn(context);
  }
}
