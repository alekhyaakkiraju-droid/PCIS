package com.pcis.batch.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.audit.domain.AuditLogRow;
import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import com.pcis.batch.audit.infrastructure.ExpiredAuditLogReader;
import com.pcis.batch.audit.job.RunLogTasklet;
import com.pcis.batch.audit.job.VerificationTasklet;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(AuditArchiveProperties.class)
public class AuditArchiveJobConfig {

  @Bean
  Clock auditArchiveClock() {
    return Clock.systemUTC();
  }

  @Bean
  OutboxEventWriter auditOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AuditArchiveProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  ExpiredAuditLogReader expiredAuditLogReader(
      DataSource dataSource, Clock clock, AuditArchiveProperties properties) {
    Instant cutoff = Instant.now(clock).minus(properties.getRetentionDays(), ChronoUnit.DAYS);
    return new ExpiredAuditLogReader(dataSource, cutoff);
  }

  @Bean
  ArchiveWriter archiveWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter auditOutboxEventWriter,
      AuditArchiveProperties properties) {
    return new ArchiveWriter(jdbcTemplate, auditOutboxEventWriter, properties);
  }

  @Bean
  Step archiveStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ExpiredAuditLogReader expiredAuditLogReader,
      ArchiveWriter archiveWriter,
      AuditArchiveProperties properties) {
    return new StepBuilder("archiveStep", jobRepository)
        .<AuditLogRow, AuditLogRow>chunk(properties.getChunkSize(), transactionManager)
        .reader(expiredAuditLogReader)
        .writer(archiveWriter)
        .build();
  }

  @Bean
  Step verifyStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      VerificationTasklet verificationTasklet) {
    return new StepBuilder("verifyStep", jobRepository)
        .tasklet(verificationTasklet, transactionManager)
        .build();
  }

  @Bean
  Step runLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      RunLogTasklet runLogTasklet) {
    return new StepBuilder("runLogStep", jobRepository)
        .tasklet(runLogTasklet, transactionManager)
        .build();
  }

  @Bean
  VerificationTasklet verificationTasklet() {
    return new VerificationTasklet();
  }

  @Bean
  RunLogTasklet runLogTasklet(JdbcTemplate jdbcTemplate, AuditArchiveProperties properties) {
    return new RunLogTasklet(jdbcTemplate, properties);
  }

  @Bean
  Job auditArchiveJob(
      JobRepository jobRepository,
      Step archiveStep,
      Step verifyStep,
      Step runLogStep,
      BatchJobExitCodeListener batchJobExitCodeListener,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("auditArchiveJob", jobRepository)
            .listener(batchJobExitCodeListener)
            .listener(batchExitCodeJobListener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(archiveStep).next(verifyStep).next(runLogStep).build();
  }
}
