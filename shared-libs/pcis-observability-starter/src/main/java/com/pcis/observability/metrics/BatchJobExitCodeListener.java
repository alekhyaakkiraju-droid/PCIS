package com.pcis.observability.metrics;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Records {@code pcis_batch_job_exit_code} when a Spring Batch job finishes.
 *
 * <p>Maps {@link BatchStatus} to the WO-137 exit code contract. Domain jobs may override exit codes
 * by setting {@code exitCode} on the job execution execution context before completion.
 */
public class BatchJobExitCodeListener implements JobExecutionListener {

  static final String EXIT_CODE_CONTEXT_KEY = "pcis.batch.exitCode";

  private final BatchJobExitCodeMetrics metrics;

  public BatchJobExitCodeListener(BatchJobExitCodeMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    int exitCode = resolveExitCode(jobExecution);
    metrics.recordExitCode(jobName, exitCode);
  }

  static int resolveExitCode(JobExecution jobExecution) {
    if (jobExecution.getExecutionContext().containsKey(EXIT_CODE_CONTEXT_KEY)) {
      return jobExecution.getExecutionContext().getInt(EXIT_CODE_CONTEXT_KEY);
    }
    BatchStatus status = jobExecution.getStatus();
    return switch (status) {
      case COMPLETED -> 0;
      case FAILED -> 1;
      case STOPPED -> 5;
      default -> 1;
    };
  }
}
