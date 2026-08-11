package com.pcis.batch.common;

/**
 * @deprecated Use {@link BatchJobExecutionListener} directly.
 */
@Deprecated
public class BatchExitCodeJobListener extends BatchJobExecutionListener {

  public BatchExitCodeJobListener(
      BatchCommonAutoConfiguration.BatchProcessExitCode exitCode, PcisBatchProperties properties) {
    super(exitCode, properties);
  }

  public BatchExitCodeJobListener(BatchCommonAutoConfiguration.BatchProcessExitCode exitCode) {
    this(exitCode, new PcisBatchProperties());
  }
}
