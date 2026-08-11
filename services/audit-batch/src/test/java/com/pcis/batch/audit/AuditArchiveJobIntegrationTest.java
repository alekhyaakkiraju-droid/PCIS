package com.pcis.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.audit.support.PostgresTestContainer;
import com.pcis.batch.audit.support.TestEnvironment;
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
@EnabledIf("com.pcis.batch.audit.support.TestEnvironment#isDockerAvailable")
class AuditArchiveJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job auditArchiveJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(auditArchiveJob);
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/mixed_age_audit_logs.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  @Test
  void archivesExpiredRowsAndSelfAuditsViaOutbox() throws Exception {
    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT_LOG_T", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT_LOG_ARCHIVE_T", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE EVENT_TYPE = 'ChunkArchived'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'AUD002B'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void leavesRecentRowsUntouched() throws Exception {
    jobLauncherTestUtils.launchJob();

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AUDIT_LOG_T WHERE PROGRAM_NAME = 'POL006B'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AUDIT_LOG_T WHERE PROGRAM_NAME = 'CUS001A'", Integer.class))
        .isEqualTo(1);
  }
}
