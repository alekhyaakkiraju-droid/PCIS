package com.pcis.billing.batch.cmm001b.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.billing.batch.cmm001b.domain.CommissionCandidateRow;
import com.pcis.billing.batch.cmm001b.domain.CommissionDecision;
import com.pcis.billing.batch.cmm001b.infrastructure.CommissionCandidateReader;
import com.pcis.billing.batch.cmm001b.infrastructure.CommissionLedgerWriter;
import com.pcis.billing.batch.cmm001b.infrastructure.CommissionProcessor;
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
@EnableConfigurationProperties(CommissionCalculationProperties.class)
public class CommissionCalculationJobConfig {

  @Bean
  OutboxEventWriter commissionOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, CommissionCalculationProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  CommissionCandidateReader commissionCandidateReader(DataSource dataSource) {
    return new CommissionCandidateReader(dataSource);
  }

  @Bean
  CommissionProcessor commissionProcessor() {
    return new CommissionProcessor();
  }

  @Bean
  CommissionLedgerWriter commissionLedgerWriter(
      JdbcTemplate jdbcTemplate,
      @Qualifier("commissionOutboxEventWriter") OutboxEventWriter commissionOutboxEventWriter,
      CommissionCalculationProperties properties) {
    return new CommissionLedgerWriter(jdbcTemplate, commissionOutboxEventWriter, properties);
  }

  @Bean
  Step commissionCalculationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      CommissionCandidateReader commissionCandidateReader,
      CommissionProcessor commissionProcessor,
      CommissionLedgerWriter commissionLedgerWriter,
      CommissionCalculationProperties properties) {
    return new StepBuilder("commissionCalculationStep", jobRepository)
        .<CommissionCandidateRow, CommissionDecision>chunk(properties.getChunkSize(), transactionManager)
        .reader(commissionCandidateReader)
        .processor(commissionProcessor)
        .writer(commissionLedgerWriter)
        .build();
  }

  @Bean
  Step commissionCalculationRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      @Qualifier("commissionCalculationRunLogTasklet") BatchRunLogTasklet commissionCalculationRunLogTasklet) {
    return BatchJobRunLogSupport.runLogStep(
        "commissionCalculationRunLogStep",
        jobRepository,
        transactionManager,
        commissionCalculationRunLogTasklet);
  }

  @Bean
  Job commissionCalculationJob(
      JobRepository jobRepository,
      @Qualifier("commissionCalculationStep") Step commissionCalculationStep,
      @Qualifier("commissionCalculationRunLogStep") Step commissionCalculationRunLogStep,
      BatchExitCodeJobListener batchExitCodeJobListener,
      ObjectProvider<BatchJobExitCodeListener> batchJobExitCodeListener,
      ObjectProvider<BatchSecurityContextInitializer> batchSecurityContextInitializer) {
    JobBuilder builder =
        new JobBuilder("commissionCalculationJob", jobRepository).listener(batchExitCodeJobListener);
    batchJobExitCodeListener.ifAvailable(builder::listener);
    batchSecurityContextInitializer.ifAvailable(builder::listener);
    return builder.start(commissionCalculationStep).next(commissionCalculationRunLogStep).build();
  }
}
