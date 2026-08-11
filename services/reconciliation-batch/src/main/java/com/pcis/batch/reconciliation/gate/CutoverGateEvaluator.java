package com.pcis.batch.reconciliation.gate;

import com.pcis.batch.reconciliation.config.ReconciliationProperties;
import com.pcis.batch.reconciliation.domain.ReconciliationRunSummary;
import org.springframework.stereotype.Component;

@Component
public class CutoverGateEvaluator {

  private final ReconciliationProperties properties;

  public CutoverGateEvaluator(ReconciliationProperties properties) {
    this.properties = properties;
  }

  public ReconciliationRunSummary.GateVerdict evaluate(
      int consecutiveCleanDays, long unexplainedBreakCount) {
    if (unexplainedBreakCount > 0) {
      return ReconciliationRunSummary.GateVerdict.FAIL;
    }
    if (consecutiveCleanDays >= properties.getMinimumCleanDays()) {
      return ReconciliationRunSummary.GateVerdict.PASS;
    }
    return ReconciliationRunSummary.GateVerdict.FAIL;
  }

  public String failureReason(int consecutiveCleanDays, long unexplainedBreakCount) {
    if (unexplainedBreakCount > 0) {
      return "Unexplained breaks present: " + unexplainedBreakCount;
    }
    return "Insufficient consecutive clean days: "
        + consecutiveCleanDays
        + " (minimum "
        + properties.getMinimumCleanDays()
        + ")";
  }
}
