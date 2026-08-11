package com.pcis.premium.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DiscountRuleRepository {

  private final JdbcClient jdbcClient;

  public DiscountRuleRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<DiscountRuleRow> findEffectiveRules(String policyType, LocalDate asOfDate) {
    return jdbcClient
        .sql(
            """
            SELECT disc_rule_id, disc_code, disc_pct, discount_type, discount_amt,
                   stacking_group, max_combined_pct, eligibility_code
            FROM discount_rule_t
            WHERE (policy_type IS NULL OR TRIM(policy_type) = :policyType)
              AND eff_date <= :asOfDate
            ORDER BY disc_rule_id
            """)
        .param("policyType", policyType)
        .param("asOfDate", asOfDate)
        .query(
            (rs, rowNum) ->
                new DiscountRuleRow(
                    rs.getLong("disc_rule_id"),
                    rs.getString("disc_code").trim(),
                    rs.getBigDecimal("disc_pct"),
                    rs.getString("discount_type"),
                    rs.getBigDecimal("discount_amt"),
                    rs.getString("stacking_group"),
                    rs.getBigDecimal("max_combined_pct"),
                    rs.getString("eligibility_code")))
        .list();
  }

  public record DiscountRuleRow(
      long ruleId,
      String code,
      BigDecimal pct,
      String discountType,
      BigDecimal flatAmount,
      String stackingGroup,
      BigDecimal maxCombinedPct,
      String eligibilityCode) {}
}
