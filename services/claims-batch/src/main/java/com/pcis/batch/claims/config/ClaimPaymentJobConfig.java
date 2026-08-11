package com.pcis.batch.claims.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.claims.domain.ClaimPaymentBatchItem;
import com.pcis.batch.claims.infrastructure.ClaimPaymentItemProcessor;
import com.pcis.batch.claims.infrastructure.ClaimPaymentJpaWriter;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.claims.application.PaymentAuthorityService;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.repository.ClaimPaymentRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.domain.repository.RecoveryRepository;
import com.pcis.claims.exception.PaymentAuthorizationException;
import com.pcis.claims.outbox.ClaimsOutboxWriter;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ClaimPaymentProperties.class)
public class ClaimPaymentJobConfig {

  @Bean
  ClaimPaymentItemProcessor claimPaymentItemProcessor(
      PaymentAuthorityService paymentAuthorityService) {
    return new ClaimPaymentItemProcessor(paymentAuthorityService);
  }

  @Bean
  ClaimPaymentJpaWriter claimPaymentJpaWriter(
      ClaimPaymentRepository claimPaymentRepository,
      ClaimReserveRepository claimReserveRepository,
      RecoveryRepository recoveryRepository,
      ClaimsOutboxWriter claimsOutboxWriter,
      ClaimPaymentProperties properties) {
    return new ClaimPaymentJpaWriter(
        claimPaymentRepository,
        claimReserveRepository,
        recoveryRepository,
        claimsOutboxWriter,
        properties);
  }

  @Bean
  Step claimPaymentStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      JpaPagingItemReader<ClaimReserveEntity> payableReserveReader,
      ClaimPaymentItemProcessor claimPaymentItemProcessor,
      ClaimPaymentJpaWriter claimPaymentJpaWriter,
      ClaimPaymentProperties properties) {
    return new StepBuilder("claimPaymentStep", jobRepository)
        .<ClaimReserveEntity, ClaimPaymentBatchItem>chunk(properties.getChunkSize(), transactionManager)
        .reader(payableReserveReader)
        .processor(claimPaymentItemProcessor)
        .writer(claimPaymentJpaWriter)
        .faultTolerant()
        .skip(PaymentAuthorizationException.class)
        .skipLimit(properties.getSkipLimit())
        .retry(TransientDataAccessException.class)
        .retryLimit(3)
        .build();
  }

  @Bean
  Step claimPaymentRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("claimPaymentRunLogTasklet") BatchRunLogTasklet claimPaymentRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "claimPaymentRunLogStep", jobRepository, transactionManager, claimPaymentRunLogTasklet);
  }

  @Bean
  Job claimPaymentJob(
      JobRepository jobRepository,
      @Qualifier("claimPaymentStep") Step claimPaymentStep,
      @Qualifier("claimPaymentRunLogStep") Step claimPaymentRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer,
      ObjectProvider<org.springframework.batch.core.JobExecutionListener> jobExecutionListeners) {
    JobBuilder builder =
        new JobBuilder("claimPaymentJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    jobExecutionListeners.orderedStream().forEach(builder::listener);
    return builder.start(claimPaymentStep).next(claimPaymentRunLogStep).build();
  }
}
