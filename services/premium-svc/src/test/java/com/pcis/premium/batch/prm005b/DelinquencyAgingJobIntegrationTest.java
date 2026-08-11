package com.pcis.premium.batch.prm005b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.premium.support.PostgresTestContainer;
import com.pcis.premium.support.TestEnvironment;
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
      "spring.flyway.enabled=false",
      "spring.autoconfigure.exclude=com.pcis.batch.auth.config.BatchAuthAutoConfiguration"
    })
@EnabledIf("com.pcis.premium.support.TestEnvironment#isDockerAvailable")
class DelinquencyAgingJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job delinquencyAgingJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(delinquencyAgingJob);
  }

  @Test
  void leavesInstallmentDueWithinGrace() throws Exception {
    loadFixtures("fixtures/prm005b-schema.sql", "fixtures/prm005b-scenario-01.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT SCHED_STATUS FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLPRM0001'",
                String.class))
        .isEqualTo("D");
  }

  @Test
  void marksInstallmentLateBeyondGrace() throws Exception {
    loadFixtures("fixtures/prm005b-schema.sql", "fixtures/prm005b-scenario-02.sql");

    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT SCHED_STATUS FROM BILLING_SCHEDULE_T WHERE POL_NBR = 'POLPRM0002'",
                String.class))
        .isEqualTo("L");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT REC_DELINQUENT FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'PRM005B'",
                Integer.class))
        .isEqualTo(1);
  }

  private void loadFixtures(String... classpaths) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    for (String path : classpaths) {
      populator.addScript(new ClassPathResource(path));
    }
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }
}
