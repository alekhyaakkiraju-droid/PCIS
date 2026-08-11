package com.pcis.batch.claims.config;

import com.pcis.batch.claims.infrastructure.ClaimPaymentJpaWriter;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ClaimPaymentRunLogConfig {

  @Bean
  @ConditionalOnMissingBean
  BatchRunLogWriter claimBatchRunLogWriter(JdbcTemplate jdbcTemplate) {
    return new BatchRunLogWriter(jdbcTemplate);
  }

  @Bean
  BatchRunLogTasklet claimPaymentRunLogTasklet(
      BatchRunLogWriter batchRunLogWriter,
      BatchRunLogConfigService batchRunLogConfigService,
      ClaimPaymentProperties properties) {
    return BatchJobRunLogSupport.tasklet(
        batchRunLogWriter,
        batchRunLogConfigService,
        properties.getProgramName(),
        chunkContext -> {
          ExecutionContext jobContext =
              chunkContext
                  .getStepContext()
                  .getStepExecution()
                  .getJobExecution()
                  .getExecutionContext();
          long selected = jobContext.getLong(ClaimPaymentJpaWriter.SELECTED_COUNT_KEY, 0L);
          long updated = jobContext.getLong(ClaimPaymentJpaWriter.UPDATED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) selected, (int) updated, errors);
        },
        () -> LocalDate.now(ZoneOffset.UTC));
  }
}
