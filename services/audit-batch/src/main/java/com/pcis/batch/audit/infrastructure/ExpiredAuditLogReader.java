package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.audit.domain.AuditLogRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;

public class ExpiredAuditLogReader extends JdbcCursorItemReader<AuditLogRow> {

  private static final String SQL =
      """
      SELECT A.LOG_ID,
             A.PROGRAM_NAME,
             A.ACTION_CODE,
             A.TABLE_NAME,
             A.RECORD_KEY,
             A.USER_ID,
             A.LOG_TIMESTAMP
      FROM AUDIT_LOG_T A
      WHERE A.LOG_TIMESTAMP < ?
        AND NOT EXISTS (
            SELECT 1
            FROM AUDIT_LOG_ARCHIVE_T X
            WHERE X.LOG_ID = A.LOG_ID
        )
      ORDER BY A.LOG_ID
      """;

  public ExpiredAuditLogReader(DataSource dataSource, Instant cutoff) {
    setDataSource(dataSource);
    setSql(SQL);
    setPreparedStatementSetter(ps -> ps.setTimestamp(1, Timestamp.from(cutoff)));
    setRowMapper(ExpiredAuditLogReader::mapRow);
    setName("expiredAuditLogReader");
  }

  private static AuditLogRow mapRow(ResultSet rs, int rowNum) throws SQLException {
    Timestamp logTimestamp = rs.getTimestamp("LOG_TIMESTAMP");
    return new AuditLogRow(
        rs.getLong("LOG_ID"),
        rs.getString("PROGRAM_NAME"),
        rs.getString("ACTION_CODE"),
        rs.getString("TABLE_NAME"),
        rs.getString("RECORD_KEY"),
        rs.getString("USER_ID"),
        logTimestamp != null ? logTimestamp.toInstant() : null);
  }
}
