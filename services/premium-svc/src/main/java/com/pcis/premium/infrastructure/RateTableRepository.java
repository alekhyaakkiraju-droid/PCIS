package com.pcis.premium.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RateTableRepository {

  private final JdbcClient jdbcClient;

  public RateTableRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Optional<RateTableRow> findEffectiveRateTable(String policyType, String territory) {
    return jdbcClient
        .sql(
            """
            SELECT rt.rate_table_id, rt.policy_type, rt.territory, rt.base_rate, rt.eff_date
            FROM rate_table_t rt
            WHERE rt.policy_type = :policyType
              AND rt.territory = :territory
            ORDER BY rt.eff_date DESC, rt.rate_table_id DESC
            LIMIT 1
            """)
        .param("policyType", policyType)
        .param("territory", territory)
        .query(
            (rs, rowNum) ->
                new RateTableRow(
                    rs.getLong("rate_table_id"),
                    rs.getString("policy_type").trim(),
                    rs.getString("territory").trim(),
                    rs.getBigDecimal("base_rate"),
                    rs.getObject("eff_date", LocalDate.class)))
        .optional();
  }

  public List<RateFactorRow> loadFactorsForRateTable(long rateTableId) {
    return jdbcClient
        .sql(
            """
            SELECT rf.rate_factor_id, rf.rate_table_id, rf.factor_code, rf.factor_value
            FROM rate_factor_t rf
            WHERE rf.rate_table_id = :rateTableId
            ORDER BY rf.rate_factor_id
            """)
        .param("rateTableId", rateTableId)
        .query(
            (rs, rowNum) ->
                new RateFactorRow(
                    rs.getLong("rate_factor_id"),
                    rs.getLong("rate_table_id"),
                    rs.getString("factor_code"),
                    rs.getBigDecimal("factor_value")))
        .list();
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

  public record RateTableRow(
      long rateTableId,
      String policyType,
      String territory,
      BigDecimal baseRate,
      LocalDate effectiveDate) {}

  public record RateFactorRow(
      long rateFactorId, long rateTableId, String factorCode, BigDecimal factorValue) {}
}
