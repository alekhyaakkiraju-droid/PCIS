package com.pcis.batch.common;

import com.pcis.observability.metrics.BatchJobExitCodeListener;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnClass(name = "org.springframework.batch.core.JobExecutionListener")
@EnableConfigurationProperties({BatchRunLogProperties.class, PcisBatchProperties.class})
public class BatchCommonAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  BatchProcessExitCode batchProcessExitCode(PcisBatchProperties properties) {
    return new BatchProcessExitCode(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  BatchJobExecutionListener batchJobExecutionListener(
      BatchProcessExitCode batchProcessExitCode, PcisBatchProperties properties) {
    return new BatchJobExecutionListener(batchProcessExitCode, properties);
  }

  @Bean
  @ConditionalOnMissingBean(name = "batchExitCodeJobListener")
  BatchExitCodeJobListener batchExitCodeJobListener(
      BatchProcessExitCode batchProcessExitCode, PcisBatchProperties properties) {
    return new BatchExitCodeJobListener(batchProcessExitCode, properties);
  }

  @Bean
  @ConditionalOnBean(JdbcTemplate.class)
  @ConditionalOnMissingBean
  BatchRunLogWriter batchRunLogWriter(JdbcTemplate jdbcTemplate) {
    return new BatchRunLogWriter(jdbcTemplate);
  }

  @Bean
  @ConditionalOnMissingBean
  BatchRunLogConfigService batchRunLogConfigService(
      org.springframework.beans.factory.ObjectProvider<com.pcis.config.TunableResolver> tunableResolver,
      BatchRunLogProperties batchRunLogProperties) {
    return new BatchRunLogConfigService(tunableResolver, batchRunLogProperties);
  }

  public static final class BatchProcessExitCode implements ExitCodeGenerator {

    private final PcisBatchProperties properties;
    private final AtomicInteger exitCode = new AtomicInteger(0);

    public BatchProcessExitCode(PcisBatchProperties properties) {
      this.properties = properties;
    }

    public void registerFromJobExecution(JobExecution jobExecution) {
      exitCode.set(BatchJobExecutionListener.resolveExitCode(jobExecution, properties.getSkipThreshold()));
    }

    public void setExitCode(int code) {
      exitCode.set(code);
    }

    @Override
    public int getExitCode() {
      return exitCode.get();
    }
  }
}
