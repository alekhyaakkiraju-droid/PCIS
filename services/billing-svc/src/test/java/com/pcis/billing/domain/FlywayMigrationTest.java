package com.pcis.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.support.BillingTestSecurityConfig;
import com.pcis.billing.support.PostgresTestContainer;
import com.pcis.billing.support.TestEnvironment;
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
@Import(BillingTestSecurityConfig.class)
@EnabledIf("com.pcis.billing.support.TestEnvironment#isDockerAvailable")
class FlywayMigrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "billing_plan_t",
          "billing_schedule_t",
          "invoice_t",
          "invoice_line_t",
          "payment_t",
          "payment_application_t",
          "refund_t",
          "outbox_events");

  @Test
  void billingDomainTablesExist() {
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
  void v2AddsInvoiceLineAndPaymentApplicationTables() {
    Integer invoiceLineCols =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'invoice_line_t'",
            Integer.class);
    assertThat(invoiceLineCols).isGreaterThan(0);

    Integer paymentAppCols =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'payment_application_t'",
            Integer.class);
    assertThat(paymentAppCols).isGreaterThan(0);
  }

  @Test
  void seqPaymentIdExists() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_name = 'seq_payment_id'",
            Integer.class);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void billingScheduleHasRecDelinquentColumn() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'billing_schedule_t' "
                + "AND column_name = 'rec_delinquent'",
            Integer.class);
    assertThat(count).isEqualTo(1);
  }
}
