package com.pcis.batch.audit.job;

import com.pcis.batch.audit.infrastructure.ArchiveRunLogWriter;
import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import com.pcis.batch.common.BatchCommonAutoConfiguration;
import com.pcis.batch.common.BatchJobExecutionListener;
import com.pcis.batch.common.PcisBatchProperties;
import java.time.Instant;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/** Persists archive_run_log summary rows and maps verification failures to exit code 2. */
@Component
public class ArchiveJobSummaryListener implements JobExecutionListener {

  public static final String VERIFICATION_STATUS_KEY = "pcis.audit.verificationStatus";

  private final ArchiveRunLogWriter archiveRunLogWriter;
  private final BatchCommonAutoConfiguration.BatchProcessExitCode batchProcessExitCode;
  private final PcisBatchProperties batchProperties;

  public ArchiveJobSummaryListener(
      ArchiveRunLogWriter archiveRunLogWriter,
      BatchCommonAutoConfiguration.BatchProcessExitCode batchProcessExitCode,
      PcisBatchProperties batchProperties) {
    this.archiveRunLogWriter = archiveRunLogWriter;
    this.batchProcessExitCode = batchProcessExitCode;
    this.batchProperties = batchProperties;
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    var context = jobExecution.getExecutionContext();
    long rowsArchived = context.getLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 0L);
    int partitionsProcessed = context.getInt(PartitionDetachTasklet.DETACHED_COUNT_KEY, 0);
    String verificationStatus = context.getString(VERIFICATION_STATUS_KEY, "SKIPPED");

    if ("MISMATCH".equals(verificationStatus)) {
      context.putInt(BatchJobExecutionListener.EXIT_CODE_CONTEXT_KEY, 2);
    }

    int exitCode =
        BatchJobExecutionListener.resolveExitCode(jobExecution, batchProperties.getSkipThreshold());
    batchProcessExitCode.setExitCode(exitCode);

    Instant start =
        jobExecution.getStartTime() != null
            ? jobExecution.getStartTime().atZone(java.time.ZoneOffset.UTC).toInstant()
            : Instant.now();
    Instant end =
        jobExecution.getEndTime() != null
            ? jobExecution.getEndTime().atZone(java.time.ZoneOffset.UTC).toInstant()
            : Instant.now();

    String errorMessage =
        jobExecution.getStatus() == BatchStatus.FAILED
            ? jobExecution.getAllFailureExceptions().stream()
                .findFirst()
                .map(Throwable::getMessage)
                .orElse("job failed")
            : null;

    archiveRunLogWriter.write(
        new ArchiveRunLogWriter.ArchiveRunLogEntry(
            jobExecution.getJobInstance().getJobName(),
            start,
            end,
            partitionsProcessed,
            rowsArchived,
            verificationStatus,
            exitCode,
            errorMessage));
  }
}
