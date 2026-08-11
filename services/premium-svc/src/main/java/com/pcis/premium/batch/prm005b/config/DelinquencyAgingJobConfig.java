package com.pcis.premium.batch.prm005b.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.common.BatchExitCodeJobListener;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.observability.metrics.BatchJobExitCodeListener;
import com.pcis.premium.batch.prm005b.domain.DelinquencyCandidateRow;
import com.pcis.premium.batch.prm005b.domain.DelinquencyDecision;
import com.pcis.premium.batch.prm005b.infrastructure.DelinquencyAgingProcessor;
import com.pcis.premium.batch.prm005b.infrastructure.DelinquencyAgingWriter;
import com.pcis.premium.batch.prm005b.infrastructure.DelinquencyCandidateReader;
import com.pcis.premium.batch.prm005b.job.DelinquencyRunLogTasklet;
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
@EnableConfigurationProperties(DelinquencyAgingProperties.class)
public class DelinquencyAgingJobConfig {

  @Bean
  OutboxEventWriter delinquencyOutboxEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, DelinquencyAgingProperties properties) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, properties.getProgramName());
  }

  @Bean
  DelinquencyCandidateReader delinquencyCandidateReader(
      DataSource dataSource, DelinquencyAgingProperties properties) {
    return new DelinquencyCandidateReader(dataSource, properties);
  }

  @Bean
  DelinquencyAgingProcessor delinquencyAgingProcessor(DelinquencyAgingProperties properties) {
    return new DelinquencyAgingProcessor(properties);
  }

  @Bean
  DelinquencyAgingWriter delinquencyAgingWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter delinquencyOutboxEventWriter,
      DelinquencyAgingProperties properties) {
    return new DelinquencyAgingWriter(jdbcTemplate, delinquencyOutboxEventWriter, properties);
  }

  @Bean
  Step delinquencyAgingStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      DelinquencyCandidateReader delinquencyCandidateReader,
      DelinquencyAgingProcessor delinquencyAgingProcessor,
      DelinquencyAgingWriter delinquencyAgingWriter,
      DelinquencyAgingProperties properties) {
    return new StepBuilder("delinquencyAgingStep", jobRepository)
        .<DelinquencyCandidateRow, DelinquencyDecision>chunk(
            properties.getChunkSize(), transactionManager)
        .reader(delinquencyCandidateReader)
        .processor(delinquencyAgingProcessor)
        .writer(delinquencyAgingWriter)
        .build();
  }

  @Bean
  Step delinquencyAgingRunLogStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      DelinquencyRunLogTasklet delinquencyRunLogTasklet) {
    return new StepBuilder("delinquencyAgingRunLogStep", jobRepository)
        .tasklet(delinquencyRunLogTasklet, transactionManager)
        .build();
  }

  @Bean
  DelinquencyRunLogTasklet delinquencyRunLogTasklet(
      JdbcTemplate jdbcTemplate, DelinquencyAgingProperties properties) {
    return new DelinquencyRunLogTasklet(jdbcTemplate, properties);
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
