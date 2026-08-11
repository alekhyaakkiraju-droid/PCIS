package com.pcis.billing.batch.bil003b.infrastructure;

import com.pcis.billing.batch.bil003b.domain.BillingCandidateRow;
import java.sql.Date;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

public class BillingCandidateReader extends JdbcCursorItemReader<BillingCandidateRow> {

  private static final String SELECT_CANDIDATES =
      """
      SELECT p.POL_NBR,
             p.PREM_ANNUAL,
             bp.BILL_FREQ,
             bp.INSTALLMENT_CNT,
             bp.BILL_PLAN_ID,
             COALESCE(MAX(bs.INSTALLMENT_NBR), 0) AS LAST_INSTALLMENT_NBR,
             MAX(bs.DUE_DATE) AS LAST_DUE_DATE
      FROM POLICY_T p
      JOIN BILLING_PLAN_T bp
        ON bp.POL_NBR = p.POL_NBR AND bp.PLAN_STATUS = 'ACTV'
      LEFT JOIN BILLING_SCHEDULE_T bs ON bs.POL_NBR = p.POL_NBR
      WHERE p.POL_STATUS = 'ACTV'
      GROUP BY p.POL_NBR, p.PREM_ANNUAL, bp.BILL_FREQ, bp.INSTALLMENT_CNT, bp.BILL_PLAN_ID
      HAVING COALESCE(MAX(bs.INSTALLMENT_NBR), 0) < bp.INSTALLMENT_CNT
      ORDER BY p.POL_NBR
      """;

  private static final RowMapper<BillingCandidateRow> ROW_MAPPER =
      (rs, rowNum) ->
          new BillingCandidateRow(
              rs.getString("POL_NBR"),
              rs.getBigDecimal("PREM_ANNUAL"),
              rs.getString("BILL_FREQ"),
              rs.getInt("INSTALLMENT_CNT"),
              rs.getLong("BILL_PLAN_ID"),
              rs.getInt("LAST_INSTALLMENT_NBR"),
              rs.getDate("LAST_DUE_DATE") != null
                  ? rs.getDate("LAST_DUE_DATE").toLocalDate()
                  : null);

  public BillingCandidateReader(DataSource dataSource) {
    setName("billingCandidateReader");
    setDataSource(dataSource);
    setSql(SELECT_CANDIDATES);
    setRowMapper(ROW_MAPPER);
  }
}
