package com.pcis.batch.common;

import java.time.LocalDate;
import java.util.function.Supplier;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.transaction.PlatformTransactionManager;

public final class BatchJobRunLogSupport {

  private BatchJobRunLogSupport() {}

  public static int countStepErrors(ChunkContext chunkContext) {
    return chunkContext.getStepContext().getStepExecution().getJobExecution().getStepExecutions().stream()
        .mapToInt(step -> step.getFailureExceptions().size())
        .sum();
  }

  public static BatchRunLogTasklet tasklet(
      BatchRunLogWriter runLogWriter,
      BatchRunLogConfigService configService,
      String programName,
      BatchRunLogCountersProvider countersProvider) {
    return tasklet(runLogWriter, configService, programName, countersProvider, LocalDate::now);
  }

  public static BatchRunLogTasklet tasklet(
      BatchRunLogWriter runLogWriter,
      BatchRunLogConfigService configService,
      String programName,
      BatchRunLogCountersProvider countersProvider,
      Supplier<LocalDate> runDateSupplier) {
    return new BatchRunLogTasklet(
        runLogWriter, configService, programName, countersProvider, runDateSupplier);
  }

  public static Step runLogStep(
      String stepName,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      Tasklet runLogTasklet) {
    return new StepBuilder(stepName, jobRepository)
        .tasklet(runLogTasklet, transactionManager)
        .build();
  }
}
