package com.pcis.batch.reconciliation.infrastructure;

import com.pcis.batch.reconciliation.domain.ReconciliationRunSummary;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReconciliationRunSummaryRepository {

  private final JdbcTemplate jdbcTemplate;

  public ReconciliationRunSummaryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long startRun(String domain, LocalDate businessDate) {
    Long runId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO reconciliation_run_summary (domain, business_date, started_at)
            VALUES (?, ?, ?)
            ON CONFLICT (domain, business_date)
            DO UPDATE SET started_at = EXCLUDED.started_at, completed_at = NULL
            RETURNING run_id
            """,
            Long.class,
            domain,
            Date.valueOf(businessDate),
            Timestamp.from(Instant.now()));
    return runId == null ? 0L : runId;
  }

  public void completeRun(ReconciliationRunSummary summary) {
    jdbcTemplate.update(
        """
        UPDATE reconciliation_run_summary
        SET completed_at = ?,
            entity_count = ?,
            rows_compared = ?,
            break_count = ?,
            unexplained_break_count = ?,
            gate_verdict = ?,
            consecutive_clean_days = ?
        WHERE run_id = ?
        """,
        summary.completedAt() == null ? null : Timestamp.from(summary.completedAt()),
        summary.entityCount(),
        summary.rowsCompared(),
        summary.breakCount(),
        summary.unexplainedBreakCount(),
        summary.gateVerdict().name(),
        summary.consecutiveCleanDays(),
        summary.runId());
  }

  public int consecutiveCleanDays(String domain) {
    List<LocalDate> cleanDates =
        jdbcTemplate.query(
            """
            SELECT business_date
            FROM reconciliation_run_summary
            WHERE domain = ?
              AND completed_at IS NOT NULL
              AND unexplained_break_count = 0
            ORDER BY business_date DESC
            """,
            (rs, rowNum) -> rs.getDate("business_date").toLocalDate(),
            domain);

    if (cleanDates.isEmpty()) {
      return 0;
    }

    LocalDate cursor = cleanDates.getFirst();
    int streak = 1;
    for (int i = 1; i < cleanDates.size(); i++) {
      LocalDate next = cleanDates.get(i);
      if (cursor.minusDays(1).equals(next)) {
        streak++;
        cursor = next;
      } else {
        break;
      }
    }
    return streak;
  }

  public long latestUnexplainedBreakCount(String domain) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT unexplained_break_count
            FROM reconciliation_run_summary
            WHERE domain = ?
            ORDER BY business_date DESC
            LIMIT 1
            """,
            Long.class,
            domain);
    return count == null ? 0L : count;
  }

  public List<String> listDomains() {
    return jdbcTemplate.query(
        """
        SELECT DISTINCT domain
        FROM reconciliation_run_summary
        ORDER BY domain
        """,
        (rs, rowNum) -> rs.getString("domain"));
  }

  public ReconciliationRunSummary findLatest(String domain) {
    return jdbcTemplate.query(
        """
        SELECT run_id, domain, business_date, started_at, completed_at,
               entity_count, rows_compared, break_count, unexplained_break_count,
               gate_verdict, consecutive_clean_days
        FROM reconciliation_run_summary
        WHERE domain = ?
        ORDER BY business_date DESC
        LIMIT 1
        """,
        rs -> {
          if (!rs.next()) {
            return null;
          }
          return new ReconciliationRunSummary(
              rs.getLong("run_id"),
              rs.getString("domain"),
              rs.getDate("business_date").toLocalDate(),
              rs.getTimestamp("started_at").toInstant(),
              rs.getTimestamp("completed_at") == null
                  ? null
                  : rs.getTimestamp("completed_at").toInstant(),
              rs.getInt("entity_count"),
              rs.getLong("rows_compared"),
              rs.getLong("break_count"),
              rs.getLong("unexplained_break_count"),
              ReconciliationRunSummary.GateVerdict.valueOf(rs.getString("gate_verdict")),
              rs.getInt("consecutive_clean_days"));
        },
        domain);
  }

  public long daysSinceLastBreak(String domain) {
    Timestamp lastBreak =
        jdbcTemplate.queryForObject(
            """
            SELECT MAX(completed_at)
            FROM reconciliation_run_summary
            WHERE domain = ?
              AND unexplained_break_count > 0
            """,
            Timestamp.class,
            domain);
    if (lastBreak == null) {
      ReconciliationRunSummary latest = findLatest(domain);
      if (latest == null || latest.completedAt() == null) {
        return 0;
      }
      return ChronoUnit.DAYS.between(latest.completedAt(), Instant.now());
    }
    return ChronoUnit.DAYS.between(lastBreak.toInstant(), Instant.now());
  }
}
