package com.pcis.premium.batch.prm005b.config;

import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import com.pcis.premium.batch.prm005b.infrastructure.DelinquencyAgingWriter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelinquencyRunLogConfig {

  @Bean
  BatchRunLogTasklet delinquencyAgingRunLogTasklet(
      BatchRunLogWriter batchRunLogWriter,
      BatchRunLogConfigService batchRunLogConfigService,
      DelinquencyAgingProperties properties) {
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
          long updated = jobContext.getLong(DelinquencyAgingWriter.UPDATED_COUNT_KEY, 0L);
          long delinquent = jobContext.getLong(DelinquencyAgingWriter.DELINQUENT_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.withDelinquent(
              (int) updated, (int) updated, errors, (int) delinquent);
        },
        properties::getReferenceDate);
  }
}
