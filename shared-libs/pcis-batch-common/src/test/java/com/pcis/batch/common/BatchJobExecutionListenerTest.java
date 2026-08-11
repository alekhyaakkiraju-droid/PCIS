package com.pcis.batch.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

class BatchJobExecutionListenerTest {

  private final PcisBatchProperties properties = new PcisBatchProperties();

  @Test
  void resolveExitCode_completedReturnsZero() {
    JobExecution execution = jobExecution(BatchStatus.COMPLETED, new ExecutionContext());
    assertThat(BatchJobExecutionListener.resolveExitCode(execution, 0)).isZero();
  }

  @Test
  void resolveExitCode_skipThresholdBreachedReturnsOne() {
    JobExecution execution = jobExecution(BatchStatus.COMPLETED, new ExecutionContext());
    StepExecution step = mock(StepExecution.class);
    when(step.getSkipCount()).thenReturn(3L);
    when(execution.getStepExecutions()).thenReturn(java.util.Set.of(step));
    assertThat(BatchJobExecutionListener.resolveExitCode(execution, 2)).isEqualTo(1);
  }

  @Test
  void resolveExitCode_outboxFailureReturnsFour() {
    ExecutionContext context = new ExecutionContext();
    context.put(BatchJobExecutionListener.OUTBOX_WRITE_FAILED_KEY, Boolean.TRUE);
    JobExecution execution = jobExecution(BatchStatus.COMPLETED, context);
    assertThat(BatchJobExecutionListener.resolveExitCode(execution, 0)).isEqualTo(4);
  }

  @Test
  void resolveExitCode_configFailureReturnsFive() {
    ExecutionContext context = new ExecutionContext();
    context.put(BatchJobExecutionListener.CONFIG_FAILURE_KEY, Boolean.TRUE);
    JobExecution execution = jobExecution(BatchStatus.STOPPED, context);
    assertThat(BatchJobExecutionListener.resolveExitCode(execution, 0)).isEqualTo(5);
  }

  @Test
  void resolveExitCode_jobSpecificOverrideReturnsTwoOrThree() {
    ExecutionContext context = new ExecutionContext();
    context.putInt(BatchJobExecutionListener.EXIT_CODE_CONTEXT_KEY, 3);
    JobExecution execution = jobExecution(BatchStatus.FAILED, context);
    assertThat(BatchJobExecutionListener.resolveExitCode(execution, 0)).isEqualTo(3);
  }

  @Test
  void afterJob_registersExitCodeOnProcessExitCodeBean() {
    BatchCommonAutoConfiguration.BatchProcessExitCode exitCode =
        new BatchCommonAutoConfiguration.BatchProcessExitCode(properties);
    BatchJobExecutionListener listener = new BatchJobExecutionListener(exitCode, properties);

    ExecutionContext context = new ExecutionContext();
    context.put(BatchJobExecutionListener.OUTBOX_WRITE_FAILED_KEY, Boolean.TRUE);
    JobExecution execution = jobExecution(BatchStatus.COMPLETED, context);

    listener.afterJob(execution);

    assertThat(exitCode.getExitCode()).isEqualTo(4);
  }

  private static JobExecution jobExecution(BatchStatus status, ExecutionContext context) {
    JobExecution execution = mock(JobExecution.class);
    JobInstance instance = mock(JobInstance.class);
    when(execution.getJobInstance()).thenReturn(instance);
    when(instance.getJobName()).thenReturn("testJob");
    when(execution.getStatus()).thenReturn(status);
    when(execution.getExecutionContext()).thenReturn(context);
    return execution;
  }
}
