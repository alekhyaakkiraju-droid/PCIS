package com.pcis.sync.watermark;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WatermarkRepository {

  private static final RowMapper<WatermarkState> ROW_MAPPER =
      (rs, rowNum) ->
          new WatermarkState(
              rs.getString("domain_name"),
              rs.getString("source_table"),
              rs.getString("watermark_column"),
              rs.getString("watermark_value"),
              toInstant(rs.getTimestamp("last_run_at")),
              rs.getString("last_run_status"),
              rs.getLong("rows_extracted"),
              rs.getLong("rows_upserted"),
              toInstant(rs.getTimestamp("updated_at")));

  private final JdbcTemplate jdbcTemplate;

  public WatermarkRepository(@Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate) {
    this.jdbcTemplate = targetJdbcTemplate;
  }

  public Optional<WatermarkState> findByDomain(String domainName) {
    var results =
        jdbcTemplate.query(
            """
            SELECT domain_name, source_table, watermark_column, watermark_value,
                   last_run_at, last_run_status, rows_extracted, rows_upserted, updated_at
            FROM sync_watermark_state
            WHERE domain_name = ?
            """,
            ROW_MAPPER,
            domainName);
    return results.stream().findFirst();
  }

  public void initialize(
      String domainName, String sourceTable, String watermarkColumn, String initialWatermark) {
    jdbcTemplate.update(
        """
        INSERT INTO sync_watermark_state
            (domain_name, source_table, watermark_column, watermark_value, updated_at)
        VALUES (?, ?, ?, ?, NOW())
        ON CONFLICT (domain_name) DO NOTHING
        """,
        domainName,
        sourceTable,
        watermarkColumn,
        initialWatermark);
  }

  public void updateAfterRun(
      String domainName,
      String watermarkValue,
      String status,
      long rowsExtracted,
      long rowsUpserted) {
    jdbcTemplate.update(
        """
        UPDATE sync_watermark_state
        SET watermark_value = ?,
            last_run_at = NOW(),
            last_run_status = ?,
            rows_extracted = ?,
            rows_upserted = ?,
            updated_at = NOW()
        WHERE domain_name = ?
        """,
        watermarkValue,
        status,
        rowsExtracted,
        rowsUpserted,
        domainName);
  }

  public long insertRunLog(String domainName, Instant startedAt) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO sync_run_log (domain_name, started_at, status)
        VALUES (?, ?, 'RUNNING')
        RETURNING run_id
        """,
        Long.class,
        domainName,
        Timestamp.from(startedAt));
  }

  public void completeRunLog(
      long runId,
      String status,
      long rowsExtracted,
      long rowsUpserted,
      String errorMessage) {
    jdbcTemplate.update(
        """
        UPDATE sync_run_log
        SET finished_at = NOW(),
            status = ?,
            rows_extracted = ?,
            rows_upserted = ?,
            error_message = ?
        WHERE run_id = ?
        """,
        status,
        rowsExtracted,
        rowsUpserted,
        errorMessage,
        runId);
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp != null ? timestamp.toInstant() : null;
  }
}
