package com.pcis.batch.policy.infrastructure;

import com.pcis.batch.policy.config.PolicyRenewalProperties;
import com.pcis.batch.policy.config.RenewalWindowConfigService;
import com.pcis.batch.policy.domain.RenewalCandidateRow;
import java.sql.Date;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

public class ExpiringPolicyReader extends JdbcCursorItemReader<RenewalCandidateRow> {

  private static final String SQL =
      """
      SELECT p.POL_NBR,
             p.CUST_ID,
             p.AGT_ID,
             p.POLICY_TYPE,
             p.EFF_DATE,
             p.EXP_DATE,
             p.PREM_ANNUAL,
             p.BILL_FREQ,
             COALESCE(pp.STATE, 'TX') AS STATE_CODE
      FROM POLICY_T p
      LEFT JOIN POLICY_PROPERTY_T pp ON pp.POL_NBR = p.POL_NBR
      WHERE p.POL_STATUS = 'ACTV'
        AND p.RENEWAL_OF_POL IS NULL
        AND p.EXP_DATE >= ?
        AND p.EXP_DATE <= ?
        AND NOT EXISTS (
            SELECT 1 FROM POLICY_T r WHERE r.RENEWAL_OF_POL = p.POL_NBR)
      ORDER BY p.POL_NBR
      """;

  public ExpiringPolicyReader(
      DataSource dataSource,
      RenewalWindowConfigService windowConfig,
      PolicyRenewalProperties properties) {
    setDataSource(dataSource);
    setSql(SQL);
    LocalDate today =
        properties.getReferenceDate() != null ? properties.getReferenceDate() : LocalDate.now();
    int windowDays = windowConfig.getRenewalWindowDays();
    setPreparedStatementSetter(
        ps -> {
          ps.setDate(1, Date.valueOf(today));
          ps.setDate(2, Date.valueOf(today.plusDays(windowDays)));
        });
    setRowMapper(rowMapper());
    setName("expiringPolicyReader");
  }

  private static RowMapper<RenewalCandidateRow> rowMapper() {
    return (rs, rowNum) ->
        new RenewalCandidateRow(
            rs.getString("POL_NBR"),
            rs.getString("CUST_ID"),
            rs.getString("AGT_ID"),
            rs.getString("POLICY_TYPE"),
            rs.getDate("EFF_DATE").toLocalDate(),
            rs.getDate("EXP_DATE").toLocalDate(),
            rs.getBigDecimal("PREM_ANNUAL"),
            rs.getString("BILL_FREQ"),
            rs.getString("STATE_CODE"));
  }
}
