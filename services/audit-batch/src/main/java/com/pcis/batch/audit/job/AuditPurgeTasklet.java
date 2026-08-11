package com.pcis.batch.audit.job;

import com.pcis.batch.audit.config.AuditPurgeProperties;
import com.pcis.batch.audit.infrastructure.DetachedPartitionPurgeService;
import com.pcis.batch.audit.infrastructure.PurgeAuditEventEmitter;
import com.pcis.batch.audit.infrastructure.S3ArchivePurgeService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditPurgeTasklet implements Tasklet {

  public static final String PARTITIONS_PURGED_KEY = "partitionsPurged";
  public static final String KEYS_SCHEDULED_KEY = "keysScheduled";

  private final DetachedPartitionPurgeService detachedPartitionPurgeService;
  private final S3ArchivePurgeService s3ArchivePurgeService;
  private final PurgeAuditEventEmitter purgeAuditEventEmitter;
  private final AuditPurgeProperties purgeProperties;
  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  public AuditPurgeTasklet(
      DetachedPartitionPurgeService detachedPartitionPurgeService,
      S3ArchivePurgeService s3ArchivePurgeService,
      PurgeAuditEventEmitter purgeAuditEventEmitter,
      AuditPurgeProperties purgeProperties,
      JdbcTemplate jdbcTemplate,
      Clock clock) {
    this.detachedPartitionPurgeService = detachedPartitionPurgeService;
    this.s3ArchivePurgeService = s3ArchivePurgeService;
    this.purgeAuditEventEmitter = purgeAuditEventEmitter;
    this.purgeProperties = purgeProperties;
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    if (!purgeProperties.isEnabled()) {
      return RepeatStatus.FINISHED;
    }

    Boolean acquired =
        jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_lock(?)", Boolean.class, purgeProperties.getAdvisoryLockKey());
    if (!Boolean.TRUE.equals(acquired)) {
      purgeAuditEventEmitter.purgeFailed("Concurrent purge job already running");
      contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
      return RepeatStatus.FINISHED;
    }

    try {
      purgeAuditEventEmitter.purgeStart();
      Instant now = Instant.now(clock);
      String actor = purgeProperties.getProgramName();
      int partitionsPurged = detachedPartitionPurgeService.purgeEligibleDetachedPartitions(now, actor);
      int keysScheduled = s3ArchivePurgeService.scheduleCryptographicErasure(now, actor);
      purgeAuditEventEmitter.purgeComplete(partitionsPurged, keysScheduled);

      var jobContext =
          chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
      jobContext.putInt(PARTITIONS_PURGED_KEY, partitionsPurged);
      jobContext.putInt(KEYS_SCHEDULED_KEY, keysScheduled);
      return RepeatStatus.FINISHED;
    } catch (RuntimeException ex) {
      purgeAuditEventEmitter.purgeFailed(ex.getMessage());
      throw ex;
    } finally {
      jdbcTemplate.queryForObject(
          "SELECT pg_advisory_unlock(?)", Boolean.class, purgeProperties.getAdvisoryLockKey());
    }
  }
}
