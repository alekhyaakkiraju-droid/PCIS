package com.pcis.batch.claims.config;

import com.pcis.batch.claims.infrastructure.ClaimPaymentWriter;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaimPaymentRunLogConfig {

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
          long selected = jobContext.getLong(ClaimPaymentWriter.SELECTED_COUNT_KEY, 0L);
          long updated = jobContext.getLong(ClaimPaymentWriter.UPDATED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) selected, (int) updated, errors);
        },
        () -> LocalDate.now(ZoneOffset.UTC));
  }
}
