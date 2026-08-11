package com.pcis.batch.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.batch.audit.config.AuditArchiveProperties;
import com.pcis.batch.audit.config.AuditPurgeProperties;
import com.pcis.batch.audit.config.RetentionConfigService;
import com.pcis.batch.audit.domain.PurgeType;
import com.pcis.config.TunableResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class S3ArchivePurgeServiceTest {

  private S3ArchiveExportRepository exportRepository;
  private S3ArchivePurgeService service;
  private KmsKeyLifecycleClient kmsClient;
  private PurgeEvidenceWriter purgeEvidenceWriter;
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    exportRepository = mock(S3ArchiveExportRepository.class);
    kmsClient = mock(KmsKeyLifecycleClient.class);
    purgeEvidenceWriter = mock(PurgeEvidenceWriter.class);
    AuditArchiveProperties properties = new AuditArchiveProperties();
    RetentionConfigService retentionConfigService =
        new RetentionConfigService(mock(TunableResolver.class), properties);
    AuditPurgeProperties purgeProperties = new AuditPurgeProperties();
    purgeProperties.setKmsWaitingPeriodDays(7);
    service =
        new S3ArchivePurgeService(
            exportRepository,
            retentionConfigService,
            kmsClient,
            purgeEvidenceWriter,
            purgeProperties);
  }

  @Test
  void expiredExportIsEligible() {
    S3ArchiveExportRow export =
        new S3ArchiveExportRow(
            1L,
            "bucket",
            "archives/2018/part-001.parquet",
            "arn:aws:kms:us-east-1:123:key/1",
            "INTERNAL",
            "audit_log_t_y2018m01",
            Instant.parse("2018-06-01T00:00:00Z"),
            365);

    assertThat(service.isExportEligible(export, Instant.now(clock))).isTrue();
  }

  @Test
  void recentExportIsNotEligible() {
    S3ArchiveExportRow export =
        new S3ArchiveExportRow(
            2L,
            "bucket",
            "archives/2025/part-recent.parquet",
            "arn:aws:kms:us-east-1:123:key/2",
            "INTERNAL",
            "audit_log_t_y2025m10",
            Instant.parse("2025-10-20T00:00:00Z"),
            200);

    assertThat(service.isExportEligible(export, Instant.now(clock))).isFalse();
  }

  @Test
  void schedulesKmsDeletionForEligibleExport() {
    S3ArchiveExportRow export =
        new S3ArchiveExportRow(
            1L,
            "bucket",
            "archives/2018/part-001.parquet",
            "arn:aws:kms:us-east-1:123:key/1",
            "INTERNAL",
            "audit_log_t_y2018m01",
            Instant.parse("2018-06-01T00:00:00Z"),
            365);
    when(exportRepository.findExpiredUnscheduled(any())).thenReturn(List.of(export));
    when(kmsClient.scheduleKeyDeletion(eq("arn:aws:kms:us-east-1:123:key/1"), eq(7)))
        .thenReturn(
            new KmsKeyDeletionSchedule(
                "arn:aws:kms:us-east-1:123:key/1", Instant.parse("2026-08-18T00:00:00Z")));

    int scheduled = service.scheduleCryptographicErasure(Instant.now(clock), "AUDPURGE");

    assertThat(scheduled).isEqualTo(1);
    verify(purgeEvidenceWriter)
        .recordPurge(
            eq(PurgeType.S3_KEY_DESTROY),
            eq("archives/2018/part-001.parquet"),
            eq("INTERNAL"),
            anyInt(),
            any(),
            eq("AUDPURGE"),
            eq(Instant.parse("2026-08-18T00:00:00Z")));
    verify(exportRepository).markPurgeScheduled(1L);
  }
}
