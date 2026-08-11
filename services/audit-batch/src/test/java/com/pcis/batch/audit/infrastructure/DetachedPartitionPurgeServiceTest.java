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
import com.pcis.config.TunableResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DetachedPartitionPurgeServiceTest {

  private JdbcTemplate jdbcTemplate;
  private DetachedPartitionPurgeService service;
  private PurgeEvidenceWriter purgeEvidenceWriter;
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    purgeEvidenceWriter = mock(PurgeEvidenceWriter.class);
    AuditArchiveProperties properties = new AuditArchiveProperties();
    RetentionConfigService retentionConfigService =
        new RetentionConfigService(mock(TunableResolver.class), properties);
    service =
        new DetachedPartitionPurgeService(
            jdbcTemplate, retentionConfigService, properties, purgeEvidenceWriter);
  }

  @Test
  void partitionWithinOneYearFloorIsNotEligible() {
    assertThat(service.isEligibleForPurge("audit_log_t_y2025m10", Instant.now(clock)))
        .isFalse();
  }

  @Test
  void oldPartitionIsEligible() {
    assertThat(service.isEligibleForPurge("audit_log_t_y2018m01", Instant.now(clock))).isTrue();
  }

  @Test
  void dropsEligibleDetachedPartition() {
    when(jdbcTemplate.queryForList(any(String.class), eq(String.class)))
        .thenReturn(java.util.List.of("audit_log_t_y2018m01"));

    int purged = service.purgeEligibleDetachedPartitions(Instant.now(clock), "AUDPURGE");

    assertThat(purged).isEqualTo(1);
    verify(jdbcTemplate).execute("DROP TABLE IF EXISTS audit_log_t_y2018m01 CASCADE");
    verify(purgeEvidenceWriter)
        .recordPurge(any(), eq("audit_log_t_y2018m01"), any(), anyInt(), any(), eq("AUDPURGE"), eq(null));
  }
}
