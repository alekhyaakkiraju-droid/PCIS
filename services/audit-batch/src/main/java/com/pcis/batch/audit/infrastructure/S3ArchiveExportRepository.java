package com.pcis.batch.audit.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class S3ArchiveExportRepository {

  private final JdbcTemplate jdbcTemplate;

  public S3ArchiveExportRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<S3ArchiveExportRow> findExpiredUnscheduled(Instant cutoff) {
    return jdbcTemplate.query(
        """
        SELECT export_id, s3_bucket, s3_key, kms_key_arn, tier, partition_name,
               exported_at, retention_days
        FROM audit_archive_export_t
        WHERE purge_scheduled = FALSE
          AND exported_at <= ?
        ORDER BY export_id
        """,
        (rs, rowNum) ->
            new S3ArchiveExportRow(
                rs.getLong("export_id"),
                rs.getString("s3_bucket"),
                rs.getString("s3_key"),
                rs.getString("kms_key_arn"),
                rs.getString("tier"),
                rs.getString("partition_name"),
                rs.getTimestamp("exported_at").toInstant(),
                rs.getInt("retention_days")),
        java.sql.Timestamp.from(cutoff));
  }

  public void markPurgeScheduled(long exportId) {
    jdbcTemplate.update(
        "UPDATE audit_archive_export_t SET purge_scheduled = TRUE WHERE export_id = ?",
        exportId);
  }
}
