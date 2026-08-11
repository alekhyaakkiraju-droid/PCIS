package com.pcis.batch.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@EnabledIf("dockerAvailable")
@Testcontainers(disabledWithoutDocker = true)
class BatchFrameworkIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("pcis_batch_common_test")
          .withUsername("pcis")
          .withPassword("pcis");

  static boolean dockerAvailable() {
    try {
      return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  private DataSource dataSource;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() throws Exception {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUsername(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    dataSource = ds;
    jdbcTemplate = new JdbcTemplate(dataSource);
    Path migrations = Path.of("db/migration").toAbsolutePath().normalize();
    if (!migrations.resolve("V1__batch_run_log.sql").toFile().exists()) {
      migrations = Path.of("shared-libs/pcis-batch-common/db/migration").toAbsolutePath().normalize();
    }
    Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:" + migrations)
        .load()
        .migrate();
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/batch_framework_seed.sql"));
    populator.execute(dataSource);
    ScriptUtils.executeSqlScript(
        dataSource.getConnection(),
        new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql"));
  }

  @Test
  void demoJob_writesOutboxAndRunLogAtomically() throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setDataSource(dataSource);
    factory.setTransactionManager(new ResourcelessTransactionManager());
    factory.afterPropertiesSet();
    JobRepository jobRepository = factory.getObject();
    PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

    BatchRunLogWriter runLogWriter = new BatchRunLogWriter(jdbcTemplate);
    ObjectProvider<com.pcis.config.TunableResolver> tunableProvider =
        new ObjectProvider<>() {
          @Override
          public com.pcis.config.TunableResolver getObject(Object... args) {
            return null;
          }

          @Override
          public com.pcis.config.TunableResolver getIfAvailable() {
            return null;
          }

          @Override
          public com.pcis.config.TunableResolver getIfUnique() {
            return null;
          }

          @Override
          public com.pcis.config.TunableResolver getObject() {
            return null;
          }
        };
    BatchRunLogConfigService configService =
        new BatchRunLogConfigService(tunableProvider, new BatchRunLogProperties());
    BatchRunLogTasklet runLogTasklet =
        new BatchRunLogTasklet(
            runLogWriter,
            configService,
            "DEMOJOB",
            chunkContext -> BatchRunLogCounters.of(3, 3, 0),
            (Supplier<LocalDate>) () -> LocalDate.of(2026, 8, 11));

    Job job =
        TestJobConfiguration.demoOutboxJob(
            jobRepository, transactionManager, dataSource, runLogTasklet);
    TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
    launcher.setJobRepository(jobRepository);
    launcher.setTaskExecutor(new SyncTaskExecutor());
    launcher.afterPropertiesSet();

    launcher.run(job, new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class))
        .isEqualTo(3);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM RPT_RUN_LOG_T", Integer.class))
        .isEqualTo(1);
  }
}
