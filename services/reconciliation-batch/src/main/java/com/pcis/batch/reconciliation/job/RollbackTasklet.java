package com.pcis.batch.reconciliation.job;

import com.pcis.batch.reconciliation.config.ReconciliationProperties;
import com.pcis.batch.reconciliation.infrastructure.DomainRollbackService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RollbackTasklet implements Tasklet {

  private final DomainRollbackService rollbackService;
  private final ReconciliationProperties properties;

  public RollbackTasklet(
      DomainRollbackService rollbackService, ReconciliationProperties properties) {
    this.rollbackService = rollbackService;
    this.properties = properties;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String domain = resolveDomain(chunkContext);
    DomainRollbackService.RollbackResult result = rollbackService.rollbackDomain(domain);
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getJobExecution()
        .getExecutionContext()
        .putString("rollback.domain", result.domain());
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getJobExecution()
        .getExecutionContext()
        .putInt("rollback.statementsExecuted", result.statementsExecuted());
    return RepeatStatus.FINISHED;
  }

  private String resolveDomain(ChunkContext chunkContext) {
    Object jobParam = chunkContext.getStepContext().getJobParameters().get("domain");
    if (jobParam != null && StringUtils.hasText(jobParam.toString())) {
      return jobParam.toString();
    }
    String configured = properties.getRollback().getDomain();
    if (StringUtils.hasText(configured)) {
      return configured;
    }
    throw new IllegalStateException("Rollback domain must be supplied via job parameter or config");
  }
}
