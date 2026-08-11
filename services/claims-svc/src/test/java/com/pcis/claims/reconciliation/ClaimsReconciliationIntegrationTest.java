package com.pcis.claims.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.claims.support.PostgresTestContainer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    properties = {
      "pcis.outbox.relay-enabled=false",
      "spring.task.scheduling.enabled=false",
      "management.endpoint.health.probes.enabled=false"
    })
@ActiveProfiles("test")
@EnabledIf("com.pcis.claims.support.TestEnvironment#isDockerAvailable")
class ClaimsReconciliationIntegrationTest {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private ClaimsReconciliationService reconciliationService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanPayments() {
    jdbcTemplate.update("DELETE FROM claim_payment");
    jdbcTemplate.update("DELETE FROM claim");
  }

  @Test
  void reconciliationPassesWithMatchingExtract() throws Exception {
    seedPayment("CLM000000501", "1500.00");
    seedPayment("CLM000000502", "750.50");

    String csv =
        """
        claim_nbr,payment_amt
        CLM000000501,1500.00
        CLM000000502,750.50
        """;
    ReconciliationReport report =
        reconciliationService.reconcile(
            new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertThat(report.overallStatus()).isEqualTo(ReconciliationReport.OverallStatus.PASS);
  }

  @Test
  void reconciliationFailsOnValueMismatch() throws Exception {
    seedPayment("CLM000000503", "100.00");

    String csv =
        """
        claim_nbr,payment_amt
        CLM000000503,100.01
        """;
    ReconciliationReport report =
        reconciliationService.reconcile(
            new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

    assertThat(report.overallStatus()).isEqualTo(ReconciliationReport.OverallStatus.FAIL);
    assertThat(report.breaks().getFirst().breakClass()).isEqualTo(BreakClass.VALUE_MISMATCH);
  }

  private void seedPayment(String claimNbr, String amount) {
    jdbcTemplate.update(
        """
        INSERT INTO claim (claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, crt_user, crt_timestamp)
        VALUES (?, 'POL000000001', 1001, '2026-03-15', 'PRP', 'O', 'test', NOW())
        ON CONFLICT (claim_nbr) DO NOTHING
        """,
        claimNbr);
    jdbcTemplate.update(
        """
        INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, crt_user, crt_timestamp)
        VALUES (?, ?::numeric, 'P', 'test', NOW())
        """,
        claimNbr,
        amount);
  }
}
