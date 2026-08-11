package com.pcis.batch.policy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.batch.policy.domain.RatingDeclinedException;
import com.pcis.batch.policy.domain.RatingUnavailableException;
import com.pcis.batch.policy.domain.RenewalCandidateRow;
import com.pcis.batch.policy.domain.RenewalDecision;
import com.pcis.batch.policy.infrastructure.ExpiringPolicyReader;
import com.pcis.batch.policy.infrastructure.RenewalPolicyWriter;
import com.pcis.batch.policy.processor.RenewalProcessor;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import java.time.Clock;
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
@EnableConfigurationProperties(PolicyRenewalProperties.class)
public class PolicyRenewalJobConfig {

  @Bean
  OutboxEventWriter policyRenewalOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PolicyRenewalProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  ExpiringPolicyReader expiringPolicyReader(
      DataSource dataSource,
      RenewalWindowConfigService windowConfig,
      PolicyRenewalProperties properties) {
    return new ExpiringPolicyReader(dataSource, windowConfig, properties);
  }

  @Bean
  RenewalPolicyWriter renewalPolicyWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter policyRenewalOutboxEventWriter,
      PolicyRenewalProperties properties) {
    return new RenewalPolicyWriter(jdbcTemplate, policyRenewalOutboxEventWriter, properties);
  }

  @Bean
  Step policyRenewalStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ExpiringPolicyReader expiringPolicyReader,
      RenewalProcessor renewalProcessor,
      RenewalPolicyWriter renewalPolicyWriter,
      PolicyRenewalProperties properties) {
    return new StepBuilder("policyRenewalStep", jobRepository)
        .<RenewalCandidateRow, RenewalDecision>chunk(properties.getChunkSize(), transactionManager)
        .reader(expiringPolicyReader)
        .processor(renewalProcessor)
        .writer(renewalPolicyWriter)
        .faultTolerant()
        .skip(RatingDeclinedException.class)
        .skip(RatingUnavailableException.class)
        .skipLimit(Integer.MAX_VALUE)
        .build();
  }

  @Bean
  Step policyRenewalRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("policyRenewalRunLogTasklet") BatchRunLogTasklet policyRenewalRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "policyRenewalRunLogStep", jobRepository, transactionManager, policyRenewalRunLogTasklet);
  }

  @Bean
  Job policyRenewalJob(
      JobRepository jobRepository,
      @Qualifier("policyRenewalStep") Step policyRenewalStep,
      @Qualifier("policyRenewalRunLogStep") Step policyRenewalRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("policyRenewalJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(policyRenewalStep).next(policyRenewalRunLogStep).build();
  }
}
