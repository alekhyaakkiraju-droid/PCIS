package com.pcis.batch.reconciliation.gate;

import com.pcis.batch.reconciliation.config.ReconciliationProperties;
import com.pcis.batch.reconciliation.domain.ReconciliationRunSummary;
import com.pcis.batch.reconciliation.infrastructure.ReconciliationRunSummaryRepository;
import com.pcis.batch.reconciliation.metrics.ReconciliationMetrics;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CutoverGateScorecardService {

  private final ReconciliationRunSummaryRepository runSummaryRepository;
  private final CutoverGateEvaluator gateEvaluator;
  private final ReconciliationProperties properties;
  private final ReconciliationMetrics metrics;

  public CutoverGateScorecardService(
      ReconciliationRunSummaryRepository runSummaryRepository,
      CutoverGateEvaluator gateEvaluator,
      ReconciliationProperties properties,
      ReconciliationMetrics metrics) {
    this.runSummaryRepository = runSummaryRepository;
    this.gateEvaluator = gateEvaluator;
    this.properties = properties;
    this.metrics = metrics;
  }

  public CutoverScorecard scorecard() {
    List<DomainScorecardEntry> domains = new ArrayList<>();
    for (String domain : runSummaryRepository.listDomains()) {
      int consecutiveCleanDays = runSummaryRepository.consecutiveCleanDays(domain);
      long unexplainedBreaks = runSummaryRepository.latestUnexplainedBreakCount(domain);
      ReconciliationRunSummary.GateVerdict verdict =
          gateEvaluator.evaluate(consecutiveCleanDays, unexplainedBreaks);
      metrics.setConsecutiveCleanDays(domain, consecutiveCleanDays);
      domains.add(
          new DomainScorecardEntry(
              domain,
              consecutiveCleanDays,
              unexplainedBreaks,
              verdict,
              verdict == ReconciliationRunSummary.GateVerdict.PASS
                  ? null
                  : gateEvaluator.failureReason(consecutiveCleanDays, unexplainedBreaks)));
    }
    return new CutoverScorecard(properties.getMinimumCleanDays(), domains);
  }

  public record CutoverScorecard(int minimumCleanDays, List<DomainScorecardEntry> domains) {}

  public record DomainScorecardEntry(
      String domain,
      int consecutiveCleanDays,
      long unexplainedBreakCount,
      ReconciliationRunSummary.GateVerdict verdict,
      String failureReason) {}
}
