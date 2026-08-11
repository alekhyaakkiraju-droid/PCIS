package com.pcis.batch.reconciliation.infrastructure;

import com.pcis.batch.reconciliation.domain.ReconciliationBreakRecord;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReconciliationBreakRepository {

  private final JdbcTemplate jdbcTemplate;

  public ReconciliationBreakRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void upsertBreak(ReconciliationBreakRecord breakRecord) {
    jdbcTemplate.update(
        """
        INSERT INTO reconciliation_break (
            run_id, domain, break_class, entity_name, business_key, column_name,
            legacy_value, target_value, approved_decision_id, first_seen_at, last_seen_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (domain, entity_name, business_key, column_name, break_class)
        DO UPDATE SET
            run_id = EXCLUDED.run_id,
            legacy_value = EXCLUDED.legacy_value,
            target_value = EXCLUDED.target_value,
            approved_decision_id = EXCLUDED.approved_decision_id,
            last_seen_at = EXCLUDED.last_seen_at
        """,
        breakRecord.runId(),
        breakRecord.domain(),
        breakRecord.breakClass().name(),
        breakRecord.entityName(),
        breakRecord.businessKey(),
        breakRecord.columnName(),
        breakRecord.legacyValue(),
        breakRecord.targetValue(),
        breakRecord.approvedDecisionId(),
        Timestamp.from(breakRecord.firstSeenAt()),
        Timestamp.from(breakRecord.lastSeenAt()));
  }

  public void upsertBreaks(List<ReconciliationBreakRecord> breaks) {
    breaks.forEach(this::upsertBreak);
  }

  public long countUnexplainedForDomain(String domain) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM reconciliation_break
            WHERE domain = ?
              AND approved_decision_id IS NULL
            """,
            Long.class,
            domain);
    return count == null ? 0L : count;
  }

  public long countByRun(long runId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM reconciliation_break WHERE run_id = ?",
            Long.class,
            runId);
    return count == null ? 0L : count;
  }

  public Instant latestBreakSeenAt(String domain) {
    Timestamp timestamp =
        jdbcTemplate.queryForObject(
            """
            SELECT MAX(last_seen_at)
            FROM reconciliation_break
            WHERE domain = ?
            """,
            Timestamp.class,
            domain);
    return timestamp == null ? null : timestamp.toInstant();
  }
}
