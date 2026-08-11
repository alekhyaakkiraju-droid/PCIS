package com.pcis.batch.reconciliation.job;

import com.pcis.batch.reconciliation.comparator.BillingDomainComparator;
import com.pcis.batch.reconciliation.domain.ReconciliationRunSummary;
import com.pcis.batch.reconciliation.gate.CutoverGateEvaluator;
import com.pcis.batch.reconciliation.infrastructure.ReconciliationBreakRepository;
import com.pcis.batch.reconciliation.infrastructure.ReconciliationRunSummaryRepository;
import com.pcis.batch.reconciliation.metrics.ReconciliationMetrics;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationSummaryTasklet implements Tasklet {

  private final BillingDomainComparator billingDomainComparator;
  private final ReconciliationRunSummaryRepository runSummaryRepository;
  private final ReconciliationBreakRepository breakRepository;
  private final CutoverGateEvaluator gateEvaluator;
  private final ReconciliationMetrics metrics;

  public ReconciliationSummaryTasklet(
      BillingDomainComparator billingDomainComparator,
      ReconciliationRunSummaryRepository runSummaryRepository,
      ReconciliationBreakRepository breakRepository,
      CutoverGateEvaluator gateEvaluator,
      ReconciliationMetrics metrics) {
    this.billingDomainComparator = billingDomainComparator;
    this.runSummaryRepository = runSummaryRepository;
    this.breakRepository = breakRepository;
    this.gateEvaluator = gateEvaluator;
    this.metrics = metrics;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var executionContext =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
    long runId = executionContext.getLong(BillingReconciliationTasklet.RUN_ID_KEY);
    String domain = billingDomainComparator.domain();
    long breakCount = breakRepository.countByRun(runId);
    long unexplainedBreakCount = breakRepository.countUnexplainedForDomain(domain);
    long rowsCompared = executionContext.getLong("reconciliation.rowsCompared");
    int consecutiveCleanDays =
        unexplainedBreakCount == 0
            ? runSummaryRepository.consecutiveCleanDays(domain) + 1
            : 0;
    ReconciliationRunSummary.GateVerdict verdict =
        gateEvaluator.evaluate(consecutiveCleanDays, unexplainedBreakCount);

    LocalDate businessDate =
        LocalDate.parse(executionContext.getString(BillingReconciliationTasklet.BUSINESS_DATE_KEY));

    ReconciliationRunSummary summary =
        new ReconciliationRunSummary(
            runId,
            domain,
            businessDate,
            Instant.now(),
            Instant.now(),
            1,
            rowsCompared,
            breakCount,
            unexplainedBreakCount,
            verdict,
            consecutiveCleanDays);

    runSummaryRepository.completeRun(summary);
    metrics.setConsecutiveCleanDays(domain, consecutiveCleanDays);
    executionContext.putString("reconciliation.gateVerdict", verdict.name());
    executionContext.putInt("reconciliation.consecutiveCleanDays", consecutiveCleanDays);
    return RepeatStatus.FINISHED;
  }
}
