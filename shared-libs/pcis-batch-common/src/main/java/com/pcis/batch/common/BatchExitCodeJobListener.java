package com.pcis.batch.common;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class BatchExitCodeJobListener implements JobExecutionListener {

  private final BatchCommonAutoConfiguration.BatchProcessExitCode exitCode;

  public BatchExitCodeJobListener(BatchCommonAutoConfiguration.BatchProcessExitCode exitCode) {
    this.exitCode = exitCode;
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    exitCode.registerFromJobExecution(jobExecution);
  }
}
