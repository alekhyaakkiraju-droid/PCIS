package com.pcis.billing.batch.prm005b.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.billing.batch.bil003b.exception.AuditFailureException;
import com.pcis.billing.batch.bil003b.exception.BusinessRuleException;
import com.pcis.billing.batch.bil003b.exception.TemporaryException;
import com.pcis.billing.batch.prm005b.domain.DelinquencyCandidateRow;
import com.pcis.billing.batch.prm005b.domain.DelinquencyUpdate;
import com.pcis.billing.batch.prm005b.infrastructure.DelinquencyAgingProcessor;
import com.pcis.billing.batch.prm005b.infrastructure.DelinquencyAgingWriter;
import com.pcis.billing.batch.prm005b.infrastructure.DelinquencyCandidateReader;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(DelinquencyAgingProperties.class)
public class DelinquencyAgingJobConfig {

  @Bean
  OutboxEventWriter delinquencyAgingOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, DelinquencyAgingProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  DelinquencyCandidateReader delinquencyCandidateReader(
      DataSource dataSource, DelinquencyAgingProperties properties) {
    return new DelinquencyCandidateReader(dataSource, properties.getReferenceDate());
  }

  @Bean
  DelinquencyAgingProcessor delinquencyAgingProcessor(
      BillingConfigProperties billingConfig, DelinquencyAgingProperties properties) {
    return new DelinquencyAgingProcessor(billingConfig, properties);
  }

  @Bean
  DelinquencyAgingWriter delinquencyAgingWriter(
      com.pcis.billing.domain.repository.BillingScheduleRepository billingScheduleRepository,
      @Qualifier("delinquencyAgingOutboxEventWriter")
          OutboxEventWriter delinquencyAgingOutboxEventWriter,
      DelinquencyAgingProperties properties) {
    return new DelinquencyAgingWriter(
        billingScheduleRepository, delinquencyAgingOutboxEventWriter, properties);
  }

  @Bean
  Step delinquencyAgingStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      DelinquencyCandidateReader delinquencyCandidateReader,
      DelinquencyAgingProcessor delinquencyAgingProcessor,
      DelinquencyAgingWriter delinquencyAgingWriter,
      DelinquencyAgingProperties properties,
      BillingConfigProperties billingConfig) {
    return new StepBuilder("delinquencyAgingStep", jobRepository)
        .<DelinquencyCandidateRow, DelinquencyUpdate>chunk(
            properties.getChunkSize(), transactionManager)
        .reader(delinquencyCandidateReader)
        .processor(delinquencyAgingProcessor)
        .writer(delinquencyAgingWriter)
        .faultTolerant()
        .skip(BusinessRuleException.class)
        .skipLimit(billingConfig.getErrorThreshold())
        .retry(TemporaryException.class)
        .retry(TransientDataAccessException.class)
        .retry(OptimisticLockingFailureException.class)
        .retryLimit(3)
        .noRetry(AuditFailureException.class)
        .build();
  }

  @Bean
  Step delinquencyAgingRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("delinquencyAgingRunLogTasklet") BatchRunLogTasklet delinquencyAgingRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "delinquencyAgingRunLogStep",
        jobRepository,
        transactionManager,
        delinquencyAgingRunLogTasklet);
  }

  @Bean
  Job delinquencyAgingJob(
      JobRepository jobRepository,
      @Qualifier("delinquencyAgingStep") Step delinquencyAgingStep,
      @Qualifier("delinquencyAgingRunLogStep") Step delinquencyAgingRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("delinquencyAgingJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(delinquencyAgingStep).next(delinquencyAgingRunLogStep).build();
  }
}
