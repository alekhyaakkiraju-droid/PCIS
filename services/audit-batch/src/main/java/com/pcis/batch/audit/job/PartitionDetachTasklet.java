package com.pcis.batch.audit.job;

import com.pcis.batch.audit.config.RetentionConfigService;
import com.pcis.batch.audit.infrastructure.PartitionRetentionService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class PartitionDetachTasklet implements Tasklet {

  public static final String DETACHED_PARTITION_COUNT_KEY = "detachedPartitionCount";
  static final String DETACHED_COUNT_KEY = DETACHED_PARTITION_COUNT_KEY;

  private final PartitionRetentionService partitionRetentionService;
  private final RetentionConfigService retentionConfigService;
  private final Clock clock;

  public PartitionDetachTasklet(
      PartitionRetentionService partitionRetentionService,
      RetentionConfigService retentionConfigService,
      Clock clock) {
    this.partitionRetentionService = partitionRetentionService;
    this.retentionConfigService = retentionConfigService;
    this.clock = clock;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    Instant cutoff =
        Instant.now(clock).minus(retentionConfigService.getRetentionDays(), ChronoUnit.DAYS);
    int detached = partitionRetentionService.detachFullyExpiredPartitions(cutoff);
    var jobContext =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
    jobContext.putInt(DETACHED_PARTITION_COUNT_KEY, detached);
    return RepeatStatus.FINISHED;
  }
}
