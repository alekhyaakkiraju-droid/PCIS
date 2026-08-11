package com.pcis.billing.batch.prm005b.infrastructure;

import com.pcis.billing.batch.prm005b.domain.DelinquencyCandidateRow;
import java.sql.Date;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

public class DelinquencyCandidateReader extends JdbcCursorItemReader<DelinquencyCandidateRow> {

  private static final RowMapper<DelinquencyCandidateRow> ROW_MAPPER =
      (rs, rowNum) ->
          new DelinquencyCandidateRow(
              rs.getLong("BILL_SCHED_ID"),
              rs.getString("POL_NBR"),
              rs.getInt("INSTALLMENT_NBR"),
              rs.getDate("DUE_DATE").toLocalDate(),
              rs.getBigDecimal("AMT_DUE"),
              rs.getBigDecimal("AMT_PAID"),
              rs.getString("SCHED_STATUS"),
              rs.getInt("REC_DELINQUENT"),
              rs.getLong("DAYS_PAST_DUE"),
              rs.getLong("VERSION"));

  public DelinquencyCandidateReader(DataSource dataSource, LocalDate referenceDate) {
    setName("delinquencyCandidateReader");
    setDataSource(dataSource);
    setSql(
        """
        SELECT bs.BILL_SCHED_ID,
               bs.POL_NBR,
               bs.INSTALLMENT_NBR,
               bs.DUE_DATE,
               bs.AMT_DUE,
               bs.AMT_PAID,
               bs.SCHED_STATUS,
               COALESCE(bs.REC_DELINQUENT, 0) AS REC_DELINQUENT,
               bs.VERSION,
               (CAST(? AS DATE) - bs.DUE_DATE) AS DAYS_PAST_DUE
        FROM BILLING_SCHEDULE_T bs
        WHERE bs.SCHED_STATUS IN ('O', 'L')
          AND bs.DUE_DATE <= CAST(? AS DATE)
        ORDER BY bs.DUE_DATE ASC, bs.BILL_SCHED_ID ASC
        """);
    setPreparedStatementSetter(
        ps -> {
          Date ref = Date.valueOf(referenceDate);
          ps.setDate(1, ref);
          ps.setDate(2, ref);
        });
    setRowMapper(ROW_MAPPER);
  }
}
