package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.audit.config.RetentionConfigService;
import com.pcis.batch.audit.domain.PurgeType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class S3ArchivePurgeService {

  private static final Logger log = LoggerFactory.getLogger(S3ArchivePurgeService.class);

  private final S3ArchiveExportRepository exportRepository;
  private final RetentionConfigService retentionConfigService;
  private final KmsKeyLifecycleClient kmsKeyLifecycleClient;
  private final PurgeEvidenceWriter purgeEvidenceWriter;
  private final int kmsWaitingPeriodDays;

  public S3ArchivePurgeService(
      S3ArchiveExportRepository exportRepository,
      RetentionConfigService retentionConfigService,
      KmsKeyLifecycleClient kmsKeyLifecycleClient,
      PurgeEvidenceWriter purgeEvidenceWriter,
      com.pcis.batch.audit.config.AuditPurgeProperties purgeProperties) {
    this.exportRepository = exportRepository;
    this.retentionConfigService = retentionConfigService;
    this.kmsKeyLifecycleClient = kmsKeyLifecycleClient;
    this.purgeEvidenceWriter = purgeEvidenceWriter;
    this.kmsWaitingPeriodDays = purgeProperties.getKmsWaitingPeriodDays();
  }

  public int scheduleCryptographicErasure(Instant now, String actor) {
    Instant scanCutoff = now;
    List<S3ArchiveExportRow> expired = exportRepository.findExpiredUnscheduled(scanCutoff);
    Set<String> scheduledKeys = new HashSet<>();
    int scheduled = 0;

    for (S3ArchiveExportRow export : expired) {
      if (!isExportEligible(export, now)) {
        continue;
      }
      if (!scheduledKeys.add(export.kmsKeyArn())) {
        exportRepository.markPurgeScheduled(export.exportId());
        continue;
      }
      try {
        KmsKeyDeletionSchedule schedule =
            kmsKeyLifecycleClient.scheduleKeyDeletion(export.kmsKeyArn(), kmsWaitingPeriodDays);
        purgeEvidenceWriter.recordPurge(
            PurgeType.S3_KEY_DESTROY,
            export.s3Key(),
            export.tier(),
            effectiveRetentionDays(export),
            now,
            actor,
            schedule.scheduledDeletionAt());
        exportRepository.markPurgeScheduled(export.exportId());
        scheduled++;
        log.info(
            "Scheduled cryptographic erasure exportId={} keyArn={} s3Key={}",
            export.exportId(),
            export.kmsKeyArn(),
            export.s3Key());
      } catch (RuntimeException ex) {
        log.error(
            "KMS schedule failed exportId={} keyArn={} error={}",
            export.exportId(),
            export.kmsKeyArn(),
            ex.getMessage(),
            ex);
        purgeEvidenceWriter.recordPurge(
            PurgeType.S3_KEY_DESTROY,
            export.s3Key() + "|FAILED:" + ex.getMessage(),
            export.tier(),
            effectiveRetentionDays(export),
            now,
            actor,
            null);
      }
    }
    return scheduled;
  }

  boolean isExportEligible(S3ArchiveExportRow export, Instant now) {
    int retentionDays = effectiveRetentionDays(export);
    Instant eligibleAt = export.exportedAt().plus(retentionDays, ChronoUnit.DAYS);
    Instant floorEligibleAt =
        export.exportedAt().plus(RetentionConfigService.POLICY_MINIMUM_RETENTION_DAYS, ChronoUnit.DAYS);
    Instant effectiveEligibleAt = eligibleAt.isAfter(floorEligibleAt) ? eligibleAt : floorEligibleAt;
    return !now.isBefore(effectiveEligibleAt);
  }

  private int effectiveRetentionDays(S3ArchiveExportRow export) {
    int configured = Math.max(export.retentionDays(), retentionConfigService.getRetentionDaysForTier(export.tier()));
    return Math.max(configured, RetentionConfigService.POLICY_MINIMUM_RETENTION_DAYS);
  }
}
