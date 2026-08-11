package com.pcis.batch.common;

import java.sql.Date;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;

public class BatchRunLogWriter {

  private static final String INSERT_RUN_LOG =
      """
      INSERT INTO RPT_RUN_LOG_T (
          PGM_NAME,
          RUN_DATE,
          REC_SELECTED,
          REC_UPDATED,
          REC_ERRORS,
          REC_DELINQUENT,
          START_TIMESTAMP,
          END_TIMESTAMP,
          CRT_USER,
          CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;

  public BatchRunLogWriter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void write(BatchRunLogEntry entry) {
    jdbcTemplate.update(
        INSERT_RUN_LOG,
        entry.programName(),
        Date.valueOf(entry.runDate()),
        entry.recSelected(),
        entry.recUpdated(),
        entry.recErrors(),
        entry.recDelinquent(),
        Timestamp.from(entry.startTimestamp()),
        Timestamp.from(entry.endTimestamp()),
        entry.crtUser(),
        Timestamp.from(entry.endTimestamp()));
  }
}
