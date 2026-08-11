package com.pcis.batch.reconciliation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.reconciliation.support.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@ActiveProfiles("test")
@EnabledIf("com.pcis.batch.reconciliation.support.TestEnvironment#isDockerAvailable")
class DomainRollbackServiceTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private DomainRollbackService rollbackService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedShadowRows() {
    jdbcTemplate.execute(
        """
        DROP TABLE IF EXISTS payment_application_t CASCADE;
        DROP TABLE IF EXISTS invoice_line_t CASCADE;
        DROP TABLE IF EXISTS invoice_t CASCADE;
        DROP TABLE IF EXISTS billing_schedule_t CASCADE;
        DROP TABLE IF EXISTS billing_plan_t CASCADE;
        CREATE TABLE payment_application_t (
            payment_app_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            crt_user VARCHAR(20)
        );
        CREATE TABLE invoice_line_t (
            invoice_line_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            crt_user VARCHAR(20)
        );
        CREATE TABLE invoice_t (
            invoice_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            crt_user VARCHAR(20)
        );
        CREATE TABLE billing_schedule_t (
            bill_sched_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            crt_user VARCHAR(20)
        );
        CREATE TABLE billing_plan_t (
            bill_plan_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            crt_user VARCHAR(20)
        );
        """);
    jdbcTemplate.update("DELETE FROM payment_application_t");
    jdbcTemplate.update("DELETE FROM invoice_line_t");
    jdbcTemplate.update("DELETE FROM invoice_t");
    jdbcTemplate.update("DELETE FROM billing_schedule_t");
    jdbcTemplate.update("DELETE FROM billing_plan_t");
    jdbcTemplate.update("INSERT INTO billing_schedule_t (crt_user) VALUES ('SHADOW_SYNC')");
    jdbcTemplate.update("INSERT INTO billing_schedule_t (crt_user) VALUES ('TEST')");
  }

  @Test
  void removesShadowBillingRows() {
    DomainRollbackService.RollbackResult result = rollbackService.rollbackDomain("billing");

    assertThat(result.statementsExecuted()).isPositive();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM billing_schedule_t WHERE crt_user = 'SHADOW_SYNC'",
                Long.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM billing_schedule_t WHERE crt_user = 'TEST'", Long.class))
        .isEqualTo(1L);
  }
}
