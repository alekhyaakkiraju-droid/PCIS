package com.pcis.billing.batch.cmm001b.infrastructure;

import com.pcis.billing.batch.cmm001b.domain.CommissionCandidateRow;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

public class CommissionCandidateReader extends JdbcCursorItemReader<CommissionCandidateRow> {

  private static final String SELECT_CANDIDATES =
      """
      SELECT bs.BILL_SCHED_ID,
             bs.POL_NBR,
             p.AGT_ID,
             bs.AMT_PAID,
             (
               SELECT cr.COMM_RATE
               FROM COMMISSION_RATE_T cr
               WHERE cr.POLICY_TYPE = p.POLICY_TYPE
                 AND cr.EFF_DATE <= CURRENT_DATE
                 AND (cr.END_DATE IS NULL OR cr.END_DATE >= CURRENT_DATE)
               ORDER BY cr.EFF_DATE DESC
               LIMIT 1
             ) AS COMM_RATE
      FROM BILLING_SCHEDULE_T bs
      JOIN POLICY_T p ON p.POL_NBR = bs.POL_NBR
      WHERE bs.SCHED_STATUS = 'P'
        AND bs.COMM_CALC_FLAG IS NULL
        AND bs.AMT_PAID IS NOT NULL
      ORDER BY bs.BILL_SCHED_ID
      """;

  private static final RowMapper<CommissionCandidateRow> ROW_MAPPER =
      (rs, rowNum) ->
          new CommissionCandidateRow(
              rs.getLong("BILL_SCHED_ID"),
              rs.getString("POL_NBR"),
              rs.getString("AGT_ID"),
              rs.getBigDecimal("AMT_PAID"),
              rs.getBigDecimal("COMM_RATE"));

  public CommissionCandidateReader(DataSource dataSource) {
    setName("commissionCandidateReader");
    setDataSource(dataSource);
    setSql(SELECT_CANDIDATES);
    setRowMapper(ROW_MAPPER);
  }
}
