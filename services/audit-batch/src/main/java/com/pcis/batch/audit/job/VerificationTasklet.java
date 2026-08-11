package com.pcis.batch.audit.job;

import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import com.pcis.batch.common.BatchJobExecutionListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class VerificationTasklet implements Tasklet {

  static final int VERIFY_MISMATCH_EXIT_CODE = 2;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
    var jobContext = jobExecution.getExecutionContext();
    long archived = jobContext.getLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 0L);
    long deleted = jobContext.getLong(ArchiveWriter.DELETED_COUNT_KEY, 0L);

    if (archived != deleted) {
      jobContext.putString(ArchiveJobSummaryListener.VERIFICATION_STATUS_KEY, "MISMATCH");
      jobContext.putInt(BatchJobExecutionListener.EXIT_CODE_CONTEXT_KEY, VERIFY_MISMATCH_EXIT_CODE);
      throw new ArchiveVerifyMismatchException(archived, deleted);
    }

    jobContext.putString(ArchiveJobSummaryListener.VERIFICATION_STATUS_KEY, "PASSED");

    return RepeatStatus.FINISHED;
  }

  public static final class ArchiveVerifyMismatchException extends RuntimeException {
    ArchiveVerifyMismatchException(long archived, long deleted) {
      super("Archive verify mismatch: archived=" + archived + ", deleted=" + deleted);
    }
  }
}
