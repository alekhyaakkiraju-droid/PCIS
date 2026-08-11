package com.pcis.policy.batch.pol006b.infrastructure;

import com.pcis.policy.batch.pol006b.config.PolicyRenewalProperties;
import com.pcis.policy.batch.pol006b.config.RenewalWindowConfigService;
import java.sql.Date;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;

/**
 * Cursor reader avoids JPA paging drift when source policies are marked {@code RNED} during the
 * step.
 */
public class ExpiringPolicyReader extends JdbcCursorItemReader<String> {

  private static final String SQL =
      """
      SELECT p.pol_nbr
      FROM policy p
      WHERE p.pol_status = 'ACTV'
        AND p.renewal_of_pol IS NULL
        AND p.exp_date >= ?
        AND p.exp_date <= ?
        AND NOT EXISTS (
            SELECT 1 FROM policy r WHERE r.renewal_of_pol = p.pol_nbr)
      ORDER BY p.pol_nbr
      """;

  public ExpiringPolicyReader(
      DataSource dataSource,
      RenewalWindowConfigService windowConfig,
      PolicyRenewalProperties properties) {
    setDataSource(dataSource);
    setSql(SQL);
    LocalDate today =
        properties.getReferenceDate() != null
            ? properties.getReferenceDate()
            : LocalDate.now();
    int windowDays = windowConfig.getRenewalWindowDays();
    LocalDate windowEnd = today.plusDays(windowDays);
    setPreparedStatementSetter(
        ps -> {
          ps.setDate(1, Date.valueOf(today));
          ps.setDate(2, Date.valueOf(windowEnd));
        });
    setRowMapper((rs, rowNum) -> rs.getString("pol_nbr"));
    setName("expiringPolicyReader");
  }
}
