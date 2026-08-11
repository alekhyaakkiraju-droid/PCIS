package com.pcis.batch.claims;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.claims.support.PostgresTestContainer;
import com.pcis.batch.claims.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@EnabledIf("com.pcis.batch.claims.support.TestEnvironment#isDockerAvailable")
class ClaimPaymentJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job claimPaymentJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    Jwt jwt =
        Jwt.withTokenValue("batch-test-token")
            .header("alg", "none")
            .subject("BATCH_SVC")
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    jobLauncherTestUtils.setJob(claimPaymentJob);
  }

  @Test
  void paysApprovedReserveAndWritesRunLog() throws Exception {
    loadFixture("fixtures/single-reserve-payment.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT reserve_status FROM claim_reserve WHERE claim_nbr = 'CLM000000101'",
                String.class))
        .isEqualTo("P");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT payment_amt FROM claim_payment WHERE claim_nbr = 'CLM000000101'",
                BigDecimal.class))
        .isEqualByComparingTo("1500.00");
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recovery", Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = 'PaymentDisbursed'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rpt_run_log_t WHERE pgm_name = 'CLM006B'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void createsRecoveryWhenReserveExceedsCessionThreshold() throws Exception {
    loadFixture("fixtures/cession-above-threshold.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recovery", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT recovery_amt FROM recovery WHERE claim_nbr = 'CLM000000701'",
                BigDecimal.class))
        .isEqualByComparingTo("100000.01");
  }

  private void loadFixture(String classpath) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpath));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }
}
