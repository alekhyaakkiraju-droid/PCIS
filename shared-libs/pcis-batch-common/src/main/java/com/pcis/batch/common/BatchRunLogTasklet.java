package com.pcis.batch.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Supplier;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class BatchRunLogTasklet implements Tasklet {

  private final BatchRunLogWriter runLogWriter;
  private final BatchRunLogConfigService configService;
  private final String programName;
  private final BatchRunLogCountersProvider countersProvider;
  private final Supplier<LocalDate> runDateSupplier;

  public BatchRunLogTasklet(
      BatchRunLogWriter runLogWriter,
      BatchRunLogConfigService configService,
      String programName,
      BatchRunLogCountersProvider countersProvider,
      Supplier<LocalDate> runDateSupplier) {
    this.runLogWriter = runLogWriter;
    this.configService = configService;
    this.programName = programName;
    this.countersProvider = countersProvider;
    this.runDateSupplier = runDateSupplier;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    if (!configService.isRunLogEnabled()) {
      return RepeatStatus.FINISHED;
    }

    BatchRunLogCounters counters = countersProvider.resolve(chunkContext);
    Instant end = Instant.now();
    Instant start =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getStartTime() != null
            ? chunkContext
                .getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getStartTime()
                .atZone(ZoneOffset.UTC)
                .toInstant()
            : end;

    runLogWriter.write(
        new BatchRunLogEntry(
            programName,
            runDateSupplier.get(),
            counters.recSelected(),
            counters.recUpdated(),
            counters.recErrors(),
            counters.recDelinquent(),
            start,
            end,
            programName));

    return RepeatStatus.FINISHED;
  }
}
