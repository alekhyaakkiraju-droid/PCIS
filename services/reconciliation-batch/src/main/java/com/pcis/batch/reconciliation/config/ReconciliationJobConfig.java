package com.pcis.batch.reconciliation.config;

import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.reconciliation.job.BillingReconciliationTasklet;
import com.pcis.batch.reconciliation.job.ReconciliationSummaryTasklet;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
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
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ReconciliationProperties.class)
public class ReconciliationJobConfig {

  @Bean
  Step billingDomainStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      BillingReconciliationTasklet billingReconciliationTasklet) {
    return new StepBuilder("billingDomainStep", jobRepository)
        .tasklet(billingReconciliationTasklet, transactionManager)
        .build();
  }

  @Bean
  Step summaryStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ReconciliationSummaryTasklet reconciliationSummaryTasklet) {
    return new StepBuilder("summaryStep", jobRepository)
        .tasklet(reconciliationSummaryTasklet, transactionManager)
        .build();
  }

  @Bean
  Job reconciliationJob(
      JobRepository jobRepository,
      @Qualifier("billingDomainStep") Step billingDomainStep,
      @Qualifier("summaryStep") Step summaryStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("reconciliationJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(billingDomainStep).next(summaryStep).build();
  }
}
