package com.pcis.policy.batch.pol006b.config;

import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import com.pcis.policy.batch.pol006b.domain.RenewalResult;
import com.pcis.policy.batch.pol006b.exception.AuditFailureException;
import com.pcis.policy.batch.pol006b.exception.PremiumServiceUnavailableException;
import com.pcis.policy.batch.pol006b.exception.RenewalException;
import com.pcis.policy.batch.pol006b.exception.TemporaryException;
import com.pcis.policy.batch.pol006b.infrastructure.ExpiringPolicyReader;
import com.pcis.policy.batch.pol006b.infrastructure.PolicyRenewalSkipListener;
import com.pcis.policy.batch.pol006b.infrastructure.RenewalProcessor;
import com.pcis.policy.batch.pol006b.infrastructure.RenewalWriter;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
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
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties({PolicyRenewalProperties.class, com.pcis.policy.config.PremiumProperties.class})
public class PolicyRenewalBatchConfig {

  @Bean
  @StepScope
  ExpiringPolicyReader expiringPolicyReader(
      DataSource dataSource,
      RenewalWindowConfigService windowConfig,
      PolicyRenewalProperties properties) {
    return new ExpiringPolicyReader(dataSource, windowConfig, properties);
  }

  @Bean
  Step policyRenewalStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ExpiringPolicyReader expiringPolicyReader,
      RenewalProcessor renewalProcessor,
      RenewalWriter renewalWriter,
      PolicyRenewalSkipListener policyRenewalSkipListener,
      PolicyRenewalProperties properties) {
    return new StepBuilder("policyRenewalStep", jobRepository)
        .<String, RenewalResult>chunk(properties.getChunkSize(), transactionManager)
        .reader(expiringPolicyReader)
        .processor(renewalProcessor)
        .writer(renewalWriter)
        .faultTolerant()
        .skip(RenewalException.class)
        .skip(PremiumServiceUnavailableException.class)
        .skipLimit(properties.getSkipLimit())
        .retry(TemporaryException.class)
        .retry(TransientDataAccessException.class)
        .retry(OptimisticLockingFailureException.class)
        .retryLimit(3)
        .noRetry(AuditFailureException.class)
        .listener(policyRenewalSkipListener)
        .build();
  }

  @Bean
  Step policyRenewalRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("policyRenewalRunLogTasklet") BatchRunLogTasklet policyRenewalRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "policyRenewalRunLogStep",
        jobRepository,
        transactionManager,
        policyRenewalRunLogTasklet);
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
