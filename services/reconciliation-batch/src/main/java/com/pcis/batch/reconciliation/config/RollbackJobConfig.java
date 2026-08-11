package com.pcis.batch.reconciliation.config;

import com.pcis.batch.reconciliation.job.RollbackTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Profile("rollback-manual")
public class RollbackJobConfig {

  @Bean
  Step rollbackStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      RollbackTasklet rollbackTasklet) {
    return new StepBuilder("rollbackStep", jobRepository)
        .tasklet(rollbackTasklet, transactionManager)
        .build();
  }

  @Bean
  Job domainRollbackJob(JobRepository jobRepository, Step rollbackStep) {
    return new JobBuilder("domainRollbackJob", jobRepository).start(rollbackStep).build();
  }
}
