package com.pcis.batch.audit.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ArchiveRunLogWriter {

  private static final String INSERT =
      """
      INSERT INTO archive_run_log (
          job_name, start_time, end_time, partitions_processed, rows_archived,
          verification_status, exit_code, error_message)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;

  public ArchiveRunLogWriter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void write(ArchiveRunLogEntry entry) {
    jdbcTemplate.update(
        INSERT,
        entry.jobName(),
        Timestamp.from(entry.startTime()),
        entry.endTime() != null ? Timestamp.from(entry.endTime()) : null,
        entry.partitionsProcessed(),
        entry.rowsArchived(),
        entry.verificationStatus(),
        entry.exitCode(),
        entry.errorMessage());
  }

  public record ArchiveRunLogEntry(
      String jobName,
      Instant startTime,
      Instant endTime,
      int partitionsProcessed,
      long rowsArchived,
      String verificationStatus,
      Integer exitCode,
      String errorMessage) {}
}
