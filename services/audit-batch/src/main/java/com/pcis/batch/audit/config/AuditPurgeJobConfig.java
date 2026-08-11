package com.pcis.batch.audit.config;

import com.pcis.batch.audit.job.AuditPurgeTasklet;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(AuditPurgeProperties.class)
public class AuditPurgeJobConfig {

  @Bean
  Step auditPurgeStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      AuditPurgeTasklet auditPurgeTasklet) {
    return new StepBuilder("auditPurgeStep", jobRepository)
        .tasklet(auditPurgeTasklet, transactionManager)
        .build();
  }

  @Bean
  Step purgeRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      BatchRunLogTasklet auditRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "purgeRunLogStep", jobRepository, transactionManager, auditRunLogTasklet);
  }

  @Bean
  @ConditionalOnProperty(name = "pcis.audit.purge.enabled", havingValue = "true", matchIfMissing = true)
  Job auditPurgeJob(
      JobRepository jobRepository,
      @Qualifier("auditPurgeStep") Step auditPurgeStep,
      @Qualifier("purgeRunLogStep") Step purgeRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener) {
    return new JobBuilder("auditPurgeJob", jobRepository)
        .listener(batchExitCodeJobListener)
        .start(auditPurgeStep)
        .next(purgeRunLogStep)
        .build();
  }
}
