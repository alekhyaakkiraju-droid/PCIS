package com.pcis.batch.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.reconciliation.domain.ReconciliationRunSummary;
import com.pcis.batch.reconciliation.support.PostgresTestContainer;
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
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.batch.job.name=reconciliationJob",
      "spring.batch.job.enabled=false",
      "pcis.reconciliation.business-date=2026-08-11",
      "pcis.reconciliation.minimum-clean-days=1"
    })
@EnabledIf("com.pcis.batch.reconciliation.support.TestEnvironment#isDockerAvailable")
class ReconciliationJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job reconciliationJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(reconciliationJob);
    runScript("fixtures/reconciliation_schema.sql");
  }

  @Test
  void reconciliationJobPassesWithMatchedBillingData() throws Exception {
    runScript("fixtures/billing_reconciliation_matched.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reconciliation_break WHERE domain = 'billing'", Long.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT gate_verdict
                FROM reconciliation_run_summary
                WHERE domain = 'billing'
                """,
                String.class))
        .isEqualTo(ReconciliationRunSummary.GateVerdict.PASS.name());
    assertThat(
            execution
                .getExecutionContext()
                .getInt("reconciliation.consecutiveCleanDays"))
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void reconciliationJobRecordsValueMismatchBreak() throws Exception {
    runScript("fixtures/billing_reconciliation_mismatch.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM reconciliation_break
                WHERE domain = 'billing'
                  AND break_class = 'VALUE_MISMATCH'
                """,
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT gate_verdict
                FROM reconciliation_run_summary
                WHERE domain = 'billing'
                """,
                String.class))
        .isEqualTo(ReconciliationRunSummary.GateVerdict.FAIL.name());
  }

  private void runScript(String classpathLocation) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpathLocation));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }
}
