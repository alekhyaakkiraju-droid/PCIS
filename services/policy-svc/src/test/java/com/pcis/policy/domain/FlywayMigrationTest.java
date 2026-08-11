package com.pcis.policy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.policy.support.PolicyTestSecurityConfig;
import com.pcis.policy.support.PostgresTestContainer;
import com.pcis.policy.support.TestEnvironment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(PolicyTestSecurityConfig.class)
@EnabledIf("com.pcis.policy.support.TestEnvironment#isDockerAvailable")
class FlywayMigrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "policy", "coverage", "coverage_type", "deductible", "policy_history",
          "policy_property", "policy_vehicle", "endorsement", "billing_plan", "outbox_events");

  @Test
  void billingPlanNotNullConstraintExists() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_name = 'billing_plan' AND constraint_type = 'FOREIGN KEY'",
            Integer.class);
    assertThat(count).isGreaterThanOrEqualTo(1);
  }

  @Test
  void allPolicyDomainTablesExist() {
    for (String table : EXPECTED_TABLES) {
      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_schema = 'public' AND table_name = ?",
              Integer.class,
              table);
      assertThat(count).as("Table '%s' should exist", table).isEqualTo(1);
    }
  }

  @Test
  void monetaryColumnsAreNumeric() {
    List<String> monetaryCols =
        jdbcTemplate.queryForList(
            "SELECT table_name || '.' || column_name FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND data_type = 'numeric' ORDER BY 1",
            String.class);
    assertThat(monetaryCols)
        .contains(
            "coverage.cov_premium", "coverage.ded_amt", "coverage.limit_amt",
            "deductible.ded_amt", "endorsement.prem_chg", "policy.prem_annual");
  }

  @Test
  void noFloatColumnsInPolicyDomain() {
    Integer floatCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'public' "
                + "AND data_type IN ('real', 'double precision', 'float') "
                + "AND table_name IN ('policy','coverage','coverage_type','deductible',"
                + "'policy_history','policy_property','policy_vehicle','endorsement')",
            Integer.class);
    assertThat(floatCount).isZero();
  }

  @Test
  void billingPlanRequiresPolicy() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_name = 'billing_plan' AND constraint_name = 'fk_billing_plan_policy'",
            Integer.class);
    assertThat(count).isEqualTo(1);
  }
}
