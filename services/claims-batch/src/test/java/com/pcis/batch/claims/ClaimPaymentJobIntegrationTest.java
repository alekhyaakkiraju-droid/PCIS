package com.pcis.batch.claims;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.claims.support.PostgresTestContainer;
import com.pcis.batch.claims.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
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
    jobLauncherTestUtils.setJob(claimPaymentJob);
  }

  @Test
  void paysApprovedReserveAndWritesRunLog() throws Exception {
    loadFixture("fixtures/single-reserve-payment.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT RESERVE_STATUS FROM CLAIM_RESERVE_T WHERE CLAIM_ID = 'CLM0001001'",
                String.class))
        .isEqualTo("PD");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT PAYMENT_AMT FROM CLAIM_PAYMENT_T WHERE CLAIM_ID = 'CLM0001001'",
                java.math.BigDecimal.class))
        .isEqualByComparingTo("1500.00");
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM RECOVERY_T", Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'ClaimPaymentProcessed'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'CLM006B'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void createsRecoveryWhenReserveExceedsCessionThreshold() throws Exception {
    loadFixture("fixtures/cession-above-threshold.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM RECOVERY_T", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT RECOVERY_AMT FROM RECOVERY_T WHERE CLAIM_ID = 'CLM0007001'",
                java.math.BigDecimal.class))
        .isEqualByComparingTo("100000.01");
  }

  private void loadFixture(String classpath) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpath));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }
}
