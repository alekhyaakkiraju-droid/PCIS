package com.pcis.batch.policy.config;

import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import com.pcis.batch.policy.infrastructure.RenewalPolicyWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicyRenewalRunLogConfig {

  @Bean
  BatchRunLogTasklet policyRenewalRunLogTasklet(
      BatchRunLogWriter batchRunLogWriter,
      BatchRunLogConfigService batchRunLogConfigService,
      PolicyRenewalProperties properties) {
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
          long selected = jobContext.getLong(RenewalPolicyWriter.SELECTED_COUNT_KEY, 0L);
          long renewed = jobContext.getLong(RenewalPolicyWriter.RENEWED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) selected, (int) renewed, errors);
        },
        () -> LocalDate.now(ZoneOffset.UTC));
  }
}
