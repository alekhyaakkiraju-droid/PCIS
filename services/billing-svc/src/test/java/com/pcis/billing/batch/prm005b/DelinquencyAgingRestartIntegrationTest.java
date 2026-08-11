package com.pcis.billing.batch.prm005b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.prm005b.config.DelinquencyAgingProperties;
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
class DelinquencyAgingRestartIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job delinquencyAgingJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private DelinquencyAgingProperties delinquencyAgingProperties;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(delinquencyAgingJob);
  }

  @Test
  void restartAfterTransientFailureProcessesAllInstallmentsWithoutDuplicates() throws Exception {
    loadFixture("fixtures/prm005b-restart-scenario.sql");

    Long failSchedId =
        jdbcTemplate.queryForObject(
            "SELECT BILL_SCHED_ID FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLDLR003'",
            Long.class);
    ReflectionTestUtils.setField(
        delinquencyAgingProperties, "failBillSchedIdForTest", failSchedId);

    JobExecution firstRun = jobLauncherTestUtils.launchJob();
    assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BILLING_SCHEDULE_T WHERE POL_NBR LIKE 'POLDLR%' AND SCHED_STATUS = 'L'",
                Integer.class))
        .isEqualTo(2);

    ReflectionTestUtils.setField(delinquencyAgingProperties, "failBillSchedIdForTest", null);
    JobExecution secondRun = jobLauncherTestUtils.launchJob();
    assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BILLING_SCHEDULE_T WHERE POL_NBR LIKE 'POLDLR%' AND SCHED_STATUS = 'L'",
                Integer.class))
        .isEqualTo(5);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM outbox_events
                WHERE EVENT_TYPE = 'DelinquencyStatusChanged'
                  AND PAYLOAD ->> 'polNbr' LIKE 'POLDLR%'
                """,
                Integer.class))
        .isEqualTo(5);
  }

  private void loadFixture(String classpath) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpath));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }
}
