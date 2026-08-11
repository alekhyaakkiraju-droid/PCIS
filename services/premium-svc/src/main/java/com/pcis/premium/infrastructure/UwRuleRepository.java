package com.pcis.premium.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UwRuleRepository {

  private final JdbcClient jdbcClient;

  public UwRuleRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<UwRuleRow> findRulesForPolicyType(String policyType) {
    return jdbcClient
        .sql(
            """
            SELECT uw_rule_id, rule_code, rule_text, rule_type, condition_field,
                   condition_operator, condition_value, outcome
            FROM uw_rule_t
            WHERE policy_type IS NULL OR TRIM(policy_type) = :policyType
            ORDER BY
              CASE rule_type WHEN 'HARD_STOP' THEN 0 WHEN 'THRESHOLD' THEN 1 ELSE 2 END,
              uw_rule_id
            """)
        .param("policyType", policyType)
        .query(
            (rs, rowNum) ->
                new UwRuleRow(
                    rs.getLong("uw_rule_id"),
                    rs.getString("rule_code").trim(),
                    rs.getString("rule_text"),
                    rs.getString("rule_type"),
                    rs.getString("condition_field"),
                    rs.getString("condition_operator"),
                    rs.getBigDecimal("condition_value"),
                    rs.getString("outcome")))
        .list();
  }

  public record UwRuleRow(
      long ruleId,
      String ruleCode,
      String ruleText,
      String ruleType,
      String conditionField,
      String conditionOperator,
      BigDecimal conditionValue,
      String outcome) {}
}
