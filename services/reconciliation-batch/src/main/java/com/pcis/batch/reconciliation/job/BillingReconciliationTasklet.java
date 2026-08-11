package com.pcis.batch.reconciliation.job;

import com.pcis.batch.reconciliation.comparator.BillingDomainComparator;
import com.pcis.batch.reconciliation.config.ReconciliationProperties;
import com.pcis.batch.reconciliation.domain.DomainComparisonResult;
import com.pcis.batch.reconciliation.domain.ReconciliationBreakRecord;
import com.pcis.batch.reconciliation.infrastructure.ReconciliationBreakRepository;
import com.pcis.batch.reconciliation.infrastructure.ReconciliationRunSummaryRepository;
import com.pcis.batch.reconciliation.metrics.ReconciliationMetrics;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDate;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class BillingReconciliationTasklet implements Tasklet {

  public static final String RUN_ID_KEY = "reconciliation.runId";
  public static final String BUSINESS_DATE_KEY = "reconciliation.businessDate";
  public static final String COMPARISON_RESULT_KEY = "reconciliation.billing.result";

  private final BillingDomainComparator billingDomainComparator;
  private final ReconciliationBreakRepository breakRepository;
  private final ReconciliationRunSummaryRepository runSummaryRepository;
  private final ReconciliationProperties properties;
  private final ReconciliationMetrics metrics;

  public BillingReconciliationTasklet(
      BillingDomainComparator billingDomainComparator,
      ReconciliationBreakRepository breakRepository,
      ReconciliationRunSummaryRepository runSummaryRepository,
      ReconciliationProperties properties,
      ReconciliationMetrics metrics) {
    this.billingDomainComparator = billingDomainComparator;
    this.breakRepository = breakRepository;
    this.runSummaryRepository = runSummaryRepository;
    this.properties = properties;
    this.metrics = metrics;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    LocalDate businessDate = resolveBusinessDate(chunkContext);
    long runId = runSummaryRepository.startRun(billingDomainComparator.domain(), businessDate);
    chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().putLong(RUN_ID_KEY, runId);
    chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put(BUSINESS_DATE_KEY, businessDate.toString());

    Timer.Sample sample = metrics.startRunTimer();
    DomainComparisonResult result = billingDomainComparator.compare(runId, businessDate);
    metrics.recordRowsCompared(result.domain(), result.rowsCompared());
    metrics.recordRunDuration(result.domain(), sample);

    for (ReconciliationBreakRecord breakRecord : result.breaks()) {
      breakRepository.upsertBreak(breakRecord);
      metrics.recordBreak(breakRecord.domain(), breakRecord.breakClass());
    }

    chunkContext
        .getStepContext()
        .getStepExecution()
        .getJobExecution()
        .getExecutionContext()
        .put(COMPARISON_RESULT_KEY, result.unexplainedBreakCount());
    executionContext(chunkContext).putLong("reconciliation.rowsCompared", result.rowsCompared());

    contribution.incrementWriteCount(result.breaks().size());
    return RepeatStatus.FINISHED;
  }

  private static org.springframework.batch.item.ExecutionContext executionContext(
      ChunkContext chunkContext) {
    return chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
  }

  private LocalDate resolveBusinessDate(ChunkContext chunkContext) {
    String configured = properties.getBusinessDate();
    if (configured != null && !configured.isBlank()) {
      return LocalDate.parse(configured);
    }
    Object jobParam =
        chunkContext.getStepContext().getJobParameters().get("businessDate");
    if (jobParam != null && !jobParam.toString().isBlank()) {
      return LocalDate.parse(jobParam.toString());
    }
    return LocalDate.now();
  }
}
