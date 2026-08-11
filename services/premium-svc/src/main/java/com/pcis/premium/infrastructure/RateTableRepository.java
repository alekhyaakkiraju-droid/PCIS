package com.pcis.premium.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RateTableRepository {

  private final JdbcClient jdbcClient;

  public RateTableRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<RateFactorRow> loadFactorsForPolicyType(String policyType) {
    return jdbcClient
        .sql(
            """
            SELECT rf.rate_factor_id, rf.rate_table_id, rf.factor_code, rf.factor_value
            FROM rate_factor_t rf
            JOIN rate_table_t rt ON rt.rate_table_id = rf.rate_table_id
            WHERE rt.policy_type = :policyType
            ORDER BY rf.rate_factor_id
            """)
        .param("policyType", policyType)
        .query(
            (rs, rowNum) ->
                new RateFactorRow(
                    rs.getLong("rate_factor_id"),
                    rs.getLong("rate_table_id"),
                    rs.getString("factor_code"),
                    rs.getBigDecimal("factor_value")))
        .list();
  }

  public record RateFactorRow(
      long rateFactorId, long rateTableId, String factorCode, BigDecimal factorValue) {}
}
