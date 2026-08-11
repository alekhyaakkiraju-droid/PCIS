package com.pcis.premium.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SurchargeRuleRepository {

  private final JdbcClient jdbcClient;

  public SurchargeRuleRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<SurchargeRuleRow> findEffectiveRules(String policyType, LocalDate asOfDate) {
    return jdbcClient
        .sql(
            """
            SELECT surch_rule_id, surch_code, surch_pct, surcharge_type, calc_type,
                   surcharge_amt, max_combined_surcharge_pct
            FROM surcharge_rule_t
            WHERE (policy_type IS NULL OR TRIM(policy_type) = :policyType)
              AND eff_date <= :asOfDate
            ORDER BY surch_rule_id
            """)
        .param("policyType", policyType)
        .param("asOfDate", asOfDate)
        .query(
            (rs, rowNum) ->
                new SurchargeRuleRow(
                    rs.getLong("surch_rule_id"),
                    rs.getString("surch_code").trim(),
                    rs.getBigDecimal("surch_pct"),
                    rs.getString("surcharge_type"),
                    rs.getString("calc_type"),
                    rs.getBigDecimal("surcharge_amt"),
                    rs.getBigDecimal("max_combined_surcharge_pct")))
        .list();
  }

  public record SurchargeRuleRow(
      long ruleId,
      String code,
      BigDecimal pct,
      String surchargeType,
      String calcType,
      BigDecimal flatAmount,
      BigDecimal maxCombinedSurchargePct) {}
}
