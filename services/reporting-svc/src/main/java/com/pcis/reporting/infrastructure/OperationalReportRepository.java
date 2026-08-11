package com.pcis.reporting.infrastructure;

import com.pcis.reporting.api.dto.AuditArchiveStatsResponse;
import com.pcis.reporting.api.dto.OperationalSummaryResponse;
import com.pcis.reporting.config.ReportingDataSourceConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
@ConditionalOnBean(name = ReportingDataSourceConfig.REPORTING_JDBC_TEMPLATE)
public class OperationalReportRepository {

  private static final String TABLE_EXISTS_QUERY =
      """
      SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = ?
      )
      """;

  private static final String RUN_LOG_SUMMARY_QUERY =
      """
      SELECT COUNT(*) AS run_count,
             COUNT(*) FILTER (WHERE REC_ERRORS > 0) AS runs_with_errors,
             MAX(RUN_DATE) AS last_run_date
      FROM RPT_RUN_LOG_T
      """;

  private static final String OPEN_BREAKS_QUERY =
      """
      SELECT COUNT(*) AS open_breaks
      FROM recon_break
      WHERE approved_decision_id IS NULL
      """;

  private static final String ARCHIVE_EXPORT_STATS_QUERY =
      """
      SELECT COUNT(*) AS export_count,
             COUNT(*) FILTER (WHERE purge_scheduled = FALSE) AS pending_purge,
             MAX(exported_at) AS last_export_at
      FROM audit_archive_export_t
      """;

  private static final String ARCHIVE_RUN_STATS_QUERY =
      """
      SELECT COUNT(*) AS run_count,
             MAX(start_time) AS last_start
      FROM archive_run_log
      WHERE job_name = 'audit-archive'
      """;

  private final JdbcTemplate jdbcTemplate;

  public OperationalReportRepository(
      @Qualifier(ReportingDataSourceConfig.REPORTING_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public OperationalSummaryResponse fetchOperationalSummary() {
    var runSummary =
        jdbcTemplate.query(
            RUN_LOG_SUMMARY_QUERY,
            rs -> {
              if (!rs.next()) {
                return new RunLogSummary(0, 0, null);
              }
              return new RunLogSummary(
                  rs.getLong("run_count"),
                  rs.getLong("runs_with_errors"),
                  toLocalDate(rs.getDate("last_run_date")));
            });

    boolean reconPresent = tableExists("recon_break");
    long openBreaks =
        reconPresent
            ? Optional.ofNullable(jdbcTemplate.queryForObject(OPEN_BREAKS_QUERY, Long.class))
                .orElse(0L)
            : 0L;

    return new OperationalSummaryResponse(
        runSummary.runCount(),
        runSummary.runsWithErrors(),
        runSummary.lastRunDate(),
        openBreaks,
        reconPresent);
  }

  public AuditArchiveStatsResponse fetchAuditArchiveStats() {
    boolean exportTablePresent = tableExists("audit_archive_export_t");
    boolean runLogPresent = tableExists("archive_run_log");

    if (!exportTablePresent && !runLogPresent) {
      return new AuditArchiveStatsResponse(0, 0, null, 0, null, false);
    }

    ArchiveExportStats exportStats =
        exportTablePresent
            ? jdbcTemplate.query(
                ARCHIVE_EXPORT_STATS_QUERY,
                rs -> {
                  if (!rs.next()) {
                    return ArchiveExportStats.empty();
                  }
                  return new ArchiveExportStats(
                      rs.getLong("export_count"),
                      rs.getLong("pending_purge"),
                      toInstant(rs.getTimestamp("last_export_at")));
                })
            : ArchiveExportStats.empty();

    ArchiveRunStats runStats =
        runLogPresent
            ? jdbcTemplate.query(
                ARCHIVE_RUN_STATS_QUERY,
                rs -> {
                  if (!rs.next()) {
                    return ArchiveRunStats.empty();
                  }
                  return new ArchiveRunStats(
                      rs.getLong("run_count"), toInstant(rs.getTimestamp("last_start")));
                })
            : ArchiveRunStats.empty();

    return new AuditArchiveStatsResponse(
        exportStats.exportCount(),
        exportStats.pendingPurge(),
        exportStats.lastExportAt(),
        runStats.runCount(),
        runStats.lastStart(),
        true);
  }

  private boolean tableExists(String tableName) {
    Boolean exists = jdbcTemplate.queryForObject(TABLE_EXISTS_QUERY, Boolean.class, tableName);
    return Boolean.TRUE.equals(exists);
  }

  private static LocalDate toLocalDate(java.sql.Date date) {
    return date == null ? null : date.toLocalDate();
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private record RunLogSummary(long runCount, long runsWithErrors, LocalDate lastRunDate) {}

  private record ArchiveExportStats(long exportCount, long pendingPurge, Instant lastExportAt) {
    static ArchiveExportStats empty() {
      return new ArchiveExportStats(0, 0, null);
    }
  }

  private record ArchiveRunStats(long runCount, Instant lastStart) {
    static ArchiveRunStats empty() {
      return new ArchiveRunStats(0, null);
    }
  }
}
