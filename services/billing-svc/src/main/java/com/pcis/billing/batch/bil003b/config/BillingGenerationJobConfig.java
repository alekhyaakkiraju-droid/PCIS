package com.pcis.billing.batch.bil003b.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.billing.batch.bil003b.domain.BillingCandidateRow;
import com.pcis.billing.batch.bil003b.domain.BillingInstallmentDecision;
import com.pcis.billing.batch.bil003b.exception.BusinessRuleException;
import com.pcis.billing.batch.bil003b.exception.TemporaryException;
import com.pcis.billing.batch.bil003b.infrastructure.BillingCandidateReader;
import com.pcis.billing.batch.bil003b.infrastructure.BillingGenerationWriter;
import com.pcis.billing.batch.bil003b.infrastructure.BillingInstallmentProcessor;
import com.pcis.billing.config.BillingConfigProperties;
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
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(BillingGenerationProperties.class)
public class BillingGenerationJobConfig {

  @Bean
  OutboxEventWriter billingGenerationOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, BillingGenerationProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  BillingCandidateReader billingCandidateReader(DataSource dataSource) {
    return new BillingCandidateReader(dataSource);
  }

  @Bean
  BillingInstallmentProcessor billingInstallmentProcessor(
      BillingGenerationProperties properties, BillingConfigProperties billingConfig) {
    return new BillingInstallmentProcessor(properties, billingConfig);
  }

  @Bean
  BillingGenerationWriter billingGenerationWriter(
      JdbcTemplate jdbcTemplate,
      @Qualifier("billingGenerationOutboxEventWriter") OutboxEventWriter billingGenerationOutboxEventWriter,
      BillingGenerationProperties properties) {
    return new BillingGenerationWriter(
        jdbcTemplate, billingGenerationOutboxEventWriter, properties);
  }

  @Bean
  Step billingGenerationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      BillingCandidateReader billingCandidateReader,
      BillingInstallmentProcessor billingInstallmentProcessor,
      BillingGenerationWriter billingGenerationWriter,
      BillingConfigProperties billingConfig) {
    return new StepBuilder("billingGenerationStep", jobRepository)
        .<BillingCandidateRow, BillingInstallmentDecision>chunk(
            billingConfig.getChunkSize(), transactionManager)
        .reader(billingCandidateReader)
        .processor(billingInstallmentProcessor)
        .writer(billingGenerationWriter)
        .faultTolerant()
        .skip(BusinessRuleException.class)
        .skipLimit(billingConfig.getErrorThreshold())
        .retry(TemporaryException.class)
        .retry(TransientDataAccessException.class)
        .retryLimit(3)
        .build();
  }

  @Bean
  Step billingGenerationRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("billingGenerationRunLogTasklet") BatchRunLogTasklet billingGenerationRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "billingGenerationRunLogStep",
        jobRepository,
        transactionManager,
        billingGenerationRunLogTasklet);
  }

  @Bean
  Job billingGenerationJob(
      JobRepository jobRepository,
      @Qualifier("billingGenerationStep") Step billingGenerationStep,
      @Qualifier("billingGenerationRunLogStep") Step billingGenerationRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("billingGenerationJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(billingGenerationStep).next(billingGenerationRunLogStep).build();
  }
}
