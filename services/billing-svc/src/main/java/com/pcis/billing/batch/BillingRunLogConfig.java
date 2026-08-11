package com.pcis.billing.batch;

import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.batch.bil003b.infrastructure.BillingGenerationWriter;
import com.pcis.billing.batch.cmm001b.config.CommissionCalculationProperties;
import com.pcis.billing.batch.cmm001b.infrastructure.CommissionLedgerWriter;
import com.pcis.billing.batch.prm005b.config.DelinquencyAgingProperties;
import com.pcis.billing.batch.prm005b.infrastructure.DelinquencyAgingWriter;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BillingRunLogConfig {

  @Bean
  BatchRunLogTasklet billingGenerationRunLogTasklet(
      BatchRunLogWriter batchRunLogWriter,
      BatchRunLogConfigService batchRunLogConfigService,
      BillingGenerationProperties properties) {
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
          long generated = jobContext.getLong(BillingGenerationWriter.GENERATED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) generated, (int) generated, errors);
        },
        properties::getReferenceDate);
  }

  @Bean
  BatchRunLogTasklet commissionCalculationRunLogTasklet(
      BatchRunLogWriter batchRunLogWriter,
      BatchRunLogConfigService batchRunLogConfigService,
      CommissionCalculationProperties properties) {
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
          long calculated = jobContext.getLong(CommissionLedgerWriter.CALCULATED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) calculated, (int) calculated, errors);
        },
        properties::getReferenceDate);
  }

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
          StepExecution agingStep =
              chunkContext
                  .getStepContext()
                  .getStepExecution()
                  .getJobExecution()
                  .getStepExecutions()
                  .stream()
                  .filter(step -> "delinquencyAgingStep".equals(step.getStepName()))
                  .findFirst()
                  .orElse(null);

          int selected = agingStep != null ? (int) agingStep.getReadCount() : 0;
          int updated = agingStep != null ? (int) agingStep.getWriteCount() : 0;
          ExecutionContext jobContext =
              chunkContext
                  .getStepContext()
                  .getStepExecution()
                  .getJobExecution()
                  .getExecutionContext();
          long delinquent =
              jobContext.getLong(DelinquencyAgingWriter.TRANSITIONED_TO_L_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.withDelinquent(selected, updated, errors, (int) delinquent);
        },
        properties::getReferenceDate);
  }
}
