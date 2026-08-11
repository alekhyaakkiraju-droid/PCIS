package com.pcis.batch.audit.job;

import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

public class VerificationTasklet implements Tasklet {

  static final int VERIFY_MISMATCH_EXIT_CODE = 2;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ExecutionContext stepContext =
        chunkContext.getStepContext().getStepExecution().getExecutionContext();
    long archived = stepContext.getLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 0L);
    long deleted = stepContext.getLong(ArchiveWriter.DELETED_COUNT_KEY, 0L);

    var jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
    jobExecution.getExecutionContext().putLong(ArchiveWriter.ARCHIVED_COUNT_KEY, archived);
    jobExecution.getExecutionContext().putLong(ArchiveWriter.DELETED_COUNT_KEY, deleted);

    if (archived != deleted) {
      jobExecution
          .getExecutionContext()
          .putInt(BatchJobExitCodeListener.EXIT_CODE_CONTEXT_KEY, VERIFY_MISMATCH_EXIT_CODE);
      throw new ArchiveVerifyMismatchException(archived, deleted);
    }

    return RepeatStatus.FINISHED;
  }

  public static final class ArchiveVerifyMismatchException extends RuntimeException {
    ArchiveVerifyMismatchException(long archived, long deleted) {
      super("Archive verify mismatch: archived=" + archived + ", deleted=" + deleted);
    }
  }
}
