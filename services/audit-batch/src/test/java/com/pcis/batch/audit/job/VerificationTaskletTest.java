package com.pcis.batch.audit.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import com.pcis.batch.common.BatchJobExecutionListener;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

class VerificationTaskletTest {

  private final VerificationTasklet tasklet = new VerificationTasklet();

  @Test
  void execute_succeedsWhenCountsMatch() throws Exception {
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();
    stepExecution
        .getJobExecution()
        .getExecutionContext()
        .putLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 2L);
    stepExecution
        .getJobExecution()
        .getExecutionContext()
        .putLong(ArchiveWriter.DELETED_COUNT_KEY, 2L);

    RepeatStatus status =
        tasklet.execute(new StepContribution(stepExecution), chunkContext(stepExecution));

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    assertThat(
            stepExecution
                .getJobExecution()
                .getExecutionContext()
                .getString(ArchiveJobSummaryListener.VERIFICATION_STATUS_KEY))
        .isEqualTo("PASSED");
  }

  @Test
  void execute_setsExitCodeTwoOnMismatch() {
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();
    stepExecution
        .getJobExecution()
        .getExecutionContext()
        .putLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 2L);
    stepExecution
        .getJobExecution()
        .getExecutionContext()
        .putLong(ArchiveWriter.DELETED_COUNT_KEY, 1L);

    assertThatThrownBy(
            () ->
                tasklet.execute(
                    new StepContribution(stepExecution), chunkContext(stepExecution)))
        .isInstanceOf(VerificationTasklet.ArchiveVerifyMismatchException.class);

    JobExecution jobExecution = stepExecution.getJobExecution();
    assertThat(
            jobExecution.getExecutionContext().getInt(BatchJobExecutionListener.EXIT_CODE_CONTEXT_KEY))
        .isEqualTo(VerificationTasklet.VERIFY_MISMATCH_EXIT_CODE);
    assertThat(
            jobExecution
                .getExecutionContext()
                .getString(ArchiveJobSummaryListener.VERIFICATION_STATUS_KEY))
        .isEqualTo("MISMATCH");
  }

  private static ChunkContext chunkContext(StepExecution stepExecution) {
    return new ChunkContext(new StepContext(stepExecution));
  }
}
