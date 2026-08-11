package com.pcis.batch.claims.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.claims.domain.ApprovedReserveRow;
import com.pcis.batch.claims.infrastructure.ApprovedReserveReader;
import com.pcis.batch.claims.infrastructure.ClaimPaymentWriter;
import com.pcis.batch.claims.job.RunLogTasklet;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ClaimPaymentProperties.class)
public class ClaimPaymentJobConfig {

  @Bean
  OutboxEventWriter claimPaymentOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, ClaimPaymentProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  ApprovedReserveReader approvedReserveReader(DataSource dataSource) {
    return new ApprovedReserveReader(dataSource);
  }

  @Bean
  ClaimPaymentWriter claimPaymentWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter claimPaymentOutboxEventWriter,
      ClaimPaymentProperties properties) {
    return new ClaimPaymentWriter(jdbcTemplate, claimPaymentOutboxEventWriter, properties);
  }

  @Bean
  Step claimPaymentStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ApprovedReserveReader approvedReserveReader,
      ClaimPaymentWriter claimPaymentWriter,
      ClaimPaymentProperties properties) {
    return new StepBuilder("claimPaymentStep", jobRepository)
        .<ApprovedReserveRow, ApprovedReserveRow>chunk(properties.getChunkSize(), transactionManager)
        .reader(approvedReserveReader)
        .writer(claimPaymentWriter)
        .build();
  }

  @Bean
  Step claimPaymentRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      RunLogTasklet claimPaymentRunLogTasklet) {
    return new StepBuilder("claimPaymentRunLogStep", jobRepository)
        .tasklet(claimPaymentRunLogTasklet, transactionManager)
        .build();
  }

  @Bean
  RunLogTasklet claimPaymentRunLogTasklet(
      JdbcTemplate jdbcTemplate, ClaimPaymentProperties properties) {
    return new RunLogTasklet(jdbcTemplate, properties);
  }

  @Bean
  Job claimPaymentJob(
      JobRepository jobRepository,
      @Qualifier("claimPaymentStep") Step claimPaymentStep,
      @Qualifier("claimPaymentRunLogStep") Step claimPaymentRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("claimPaymentJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(claimPaymentStep).next(claimPaymentRunLogStep).build();
  }
}
