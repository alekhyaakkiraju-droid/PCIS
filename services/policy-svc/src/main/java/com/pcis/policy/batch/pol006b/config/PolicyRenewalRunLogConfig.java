package com.pcis.policy.batch.pol006b.config;

import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import com.pcis.policy.batch.pol006b.infrastructure.RenewalWriter;
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
          long selected =
              chunkContext
                  .getStepContext()
                  .getStepExecution()
                  .getJobExecution()
                  .getStepExecutions()
                  .stream()
                  .filter(step -> "policyRenewalStep".equals(step.getStepName()))
                  .mapToLong(step -> step.getReadCount())
                  .findFirst()
                  .orElse(0L);
          long renewed = jobContext.getLong(RenewalWriter.RENEWED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) selected, (int) renewed, errors);
        },
        () ->
            properties.getReferenceDate() != null
                ? properties.getReferenceDate()
                : LocalDate.now(ZoneOffset.UTC));
  }
}
