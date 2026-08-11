package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.audit.domain.PurgeEvidenceRecord;
import com.pcis.batch.audit.domain.PurgeType;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PurgeEvidenceWriter {

  private final JdbcTemplate jdbcTemplate;

  public PurgeEvidenceWriter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void recordPurge(
      PurgeType purgeType,
      String targetIdentifier,
      String tier,
      int retentionDays,
      Instant purgeTimestamp,
      String actor,
      Instant scheduledDeletionAt) {
    String evidenceHash =
        PurgeEvidenceHasher.computeHash(
            purgeType, targetIdentifier, tier, retentionDays, purgeTimestamp, actor);
    jdbcTemplate.update(
        """
        INSERT INTO purge_evidence (
            purge_type, target_identifier, tier, retention_days,
            purge_timestamp, actor, evidence_hash, scheduled_deletion_at)
        VALUES (?::purge_type, ?, ?, ?, ?, ?, ?, ?)
        """,
        purgeType.name(),
        targetIdentifier,
        tier,
        retentionDays,
        java.sql.Timestamp.from(purgeTimestamp),
        actor,
        evidenceHash,
        scheduledDeletionAt == null
            ? null
            : java.sql.Timestamp.from(scheduledDeletionAt));
  }

  public void record(PurgeEvidenceRecord record) {
    recordPurge(
        record.purgeType(),
        record.targetIdentifier(),
        record.tier(),
        record.retentionDays(),
        record.purgeTimestamp(),
        record.actor(),
        record.scheduledDeletionAt());
  }
}
