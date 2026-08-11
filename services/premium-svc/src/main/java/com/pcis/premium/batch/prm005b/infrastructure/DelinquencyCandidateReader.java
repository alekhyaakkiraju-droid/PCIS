package com.pcis.premium.batch.prm005b.infrastructure;

import com.pcis.premium.batch.prm005b.config.DelinquencyAgingProperties;
import com.pcis.premium.batch.prm005b.domain.DelinquencyCandidateRow;
import java.sql.Date;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

public class DelinquencyCandidateReader extends JdbcCursorItemReader<DelinquencyCandidateRow> {

  private static final RowMapper<DelinquencyCandidateRow> ROW_MAPPER =
      (rs, rowNum) ->
          new DelinquencyCandidateRow(
              rs.getLong("BILL_SCHED_ID"),
              rs.getString("POL_NBR"),
              rs.getDate("DUE_DATE").toLocalDate(),
              rs.getBigDecimal("AMT_DUE"),
              rs.getBigDecimal("AMT_PAID"),
              rs.getString("SCHED_STATUS"),
              rs.getInt("DAYS_PAST_DUE"));

  public DelinquencyCandidateReader(DataSource dataSource, DelinquencyAgingProperties properties) {
    setName("delinquencyCandidateReader");
    setDataSource(dataSource);
    setSql(
        """
        SELECT bs.BILL_SCHED_ID,
               bs.POL_NBR,
               bs.DUE_DATE,
               bs.AMT_DUE,
               bs.AMT_PAID,
               bs.SCHED_STATUS,
               (?::date - bs.DUE_DATE)::int AS DAYS_PAST_DUE
        FROM BILLING_SCHEDULE_T bs
        WHERE bs.SCHED_STATUS IN ('D', 'L', 'O')
          AND bs.DUE_DATE <= ?::date
        ORDER BY bs.BILL_SCHED_ID
        """);
    setPreparedStatementSetter(
        ps -> {
          Date ref = Date.valueOf(properties.getReferenceDate());
          ps.setDate(1, ref);
          ps.setDate(2, ref);
        });
    setRowMapper(ROW_MAPPER);
  }
}
