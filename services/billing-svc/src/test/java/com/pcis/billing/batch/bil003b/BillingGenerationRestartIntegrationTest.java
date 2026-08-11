package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.support.PostgresTestContainer;
import com.pcis.billing.support.TestEnvironment;
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
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@EnabledIf("com.pcis.billing.support.TestEnvironment#isDockerAvailable")
class BillingGenerationRestartIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job billingGenerationJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private BillingGenerationProperties billingGenerationProperties;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(billingGenerationJob);
    ReflectionTestUtils.setField(billingGenerationProperties, "failPolicyForTest", "POLRES03");
  }

  @Test
  void restartAfterTransientFailureCreatesAllInstallmentsWithoutDuplicates() throws Exception {
    loadFixture("fixtures/bil003b-restart-scenario.sql");

    JobExecution firstRun = jobLauncherTestUtils.launchJob();
    assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM BILLING_SCHEDULE_T", Integer.class))
        .isEqualTo(2);

    ReflectionTestUtils.setField(billingGenerationProperties, "failPolicyForTest", null);
    JobExecution secondRun = jobLauncherTestUtils.launchJob();
    assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM BILLING_SCHEDULE_T", Integer.class))
        .isEqualTo(5);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM (
                  SELECT POL_NBR, INSTALLMENT_NBR
                  FROM BILLING_SCHEDULE_T
                  GROUP BY POL_NBR, INSTALLMENT_NBR
                  HAVING COUNT(*) > 1
                ) dup
                """,
                Integer.class))
        .isZero();
  }

  private void loadFixture(String classpath) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpath));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }
}
