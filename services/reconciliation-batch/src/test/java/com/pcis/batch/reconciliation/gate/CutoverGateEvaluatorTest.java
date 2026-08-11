package com.pcis.batch.reconciliation.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.reconciliation.config.ReconciliationProperties;
import com.pcis.batch.reconciliation.domain.ReconciliationRunSummary;
import org.junit.jupiter.api.Test;

class CutoverGateEvaluatorTest {

  @Test
  void passesWhenCleanDaysMetAndNoBreaks() {
    CutoverGateEvaluator evaluator = new CutoverGateEvaluator(properties(30));
    assertThat(evaluator.evaluate(30, 0))
        .isEqualTo(ReconciliationRunSummary.GateVerdict.PASS);
  }

  @Test
  void failsWhenUnexplainedBreaksPresent() {
    CutoverGateEvaluator evaluator = new CutoverGateEvaluator(properties(30));
    assertThat(evaluator.evaluate(40, 2))
        .isEqualTo(ReconciliationRunSummary.GateVerdict.FAIL);
    assertThat(evaluator.failureReason(40, 2)).contains("Unexplained breaks");
  }

  @Test
  void failsWhenCleanDayWindowNotMet() {
    CutoverGateEvaluator evaluator = new CutoverGateEvaluator(properties(30));
    assertThat(evaluator.evaluate(10, 0))
        .isEqualTo(ReconciliationRunSummary.GateVerdict.FAIL);
    assertThat(evaluator.failureReason(10, 0)).contains("Insufficient consecutive clean days");
  }

  private static ReconciliationProperties properties(int minimumCleanDays) {
    ReconciliationProperties properties = new ReconciliationProperties();
    properties.setMinimumCleanDays(minimumCleanDays);
    return properties;
  }
}
