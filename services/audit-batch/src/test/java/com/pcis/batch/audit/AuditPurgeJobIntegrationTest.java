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
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.batch.job.name=auditPurgeJob"})
@EnabledIf("com.pcis.batch.audit.support.TestEnvironment#isDockerAvailable")
class AuditPurgeJobIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job auditPurgeJob;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(auditPurgeJob);
    jdbcTemplate.execute("ALTER TABLE purge_evidence DISABLE TRIGGER trg_purge_evidence_immutable");
    jdbcTemplate.execute("TRUNCATE purge_evidence RESTART IDENTITY");
    jdbcTemplate.execute("ALTER TABLE purge_evidence ENABLE TRIGGER trg_purge_evidence_immutable");
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/purge_detached_partitions.sql"));
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
  }

  @Test
  void dropsOldDetachedPartitionAndRecordsEvidence() throws Exception {
    JobExecution execution = jobLauncherTestUtils.launchJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT to_regclass('audit_log_t_y2018m01') IS NOT NULL", Boolean.class))
        .isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM purge_evidence WHERE target_identifier = 'audit_log_t_y2018m01'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT to_regclass('audit_log_t_y2025m10') IS NOT NULL", Boolean.class))
        .isTrue();
  }

  @Test
  void schedulesKmsDestructionForExpiredExport() throws Exception {
    jobLauncherTestUtils.launchJob();

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM purge_evidence
                WHERE purge_type = 'S3_KEY_DESTROY'
                  AND target_identifier LIKE '%part-001%'
                """,
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT purge_scheduled FROM audit_archive_export_t WHERE s3_key LIKE '%part-001%'",
                Boolean.class))
        .isTrue();
  }
}
