package com.pcis.batch.common;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.BatchStatus;

/**
 * Enforces the WO-205 / WO-137 batch exit-code contract via {@link
 * BatchCommonAutoConfiguration.BatchProcessExitCode}.
 *
 * <p>Batch applications call {@code System.exit(SpringApplication.exit(...))} after the job
 * completes; this listener records the resolved code for the {@link
 * org.springframework.boot.ExitCodeGenerator}.
 */
public class BatchJobExecutionListener implements JobExecutionListener {

  public static final String EXIT_CODE_CONTEXT_KEY = "pcis.batch.exitCode";
  public static final String OUTBOX_WRITE_FAILED_KEY = "pcis.batch.outboxWriteFailed";
  public static final String CONFIG_FAILURE_KEY = "pcis.batch.configFailure";

  private final BatchCommonAutoConfiguration.BatchProcessExitCode exitCode;
  private final PcisBatchProperties properties;

  public BatchJobExecutionListener(
      BatchCommonAutoConfiguration.BatchProcessExitCode exitCode, PcisBatchProperties properties) {
    this.exitCode = exitCode;
    this.properties = properties;
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    int code = resolveExitCode(jobExecution, properties.getSkipThreshold());
    exitCode.setExitCode(code);
  }

  public static int resolveExitCode(JobExecution jobExecution, int skipThreshold) {
    var context = jobExecution.getExecutionContext();
    if (context.containsKey(EXIT_CODE_CONTEXT_KEY)) {
      return context.getInt(EXIT_CODE_CONTEXT_KEY);
    }
    if (Boolean.TRUE.equals(context.get(OUTBOX_WRITE_FAILED_KEY))) {
      return 4;
    }
    if (Boolean.TRUE.equals(context.get(CONFIG_FAILURE_KEY))) {
      return 5;
    }
    if (totalSkips(jobExecution) > skipThreshold) {
      return 1;
    }
    BatchStatus status = jobExecution.getStatus();
    return switch (status) {
      case COMPLETED -> 0;
      case STOPPED -> 5;
      case FAILED -> 1;
      default -> 1;
    };
  }

  private static long totalSkips(JobExecution jobExecution) {
    return jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum();
  }
}
