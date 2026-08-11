package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import com.pcis.batch.reconciliation.domain.ReconciliationBreakRecord;
import java.time.Instant;

/** Base type for the six reconciliation break classifiers (WO-215). */
public abstract class BreakClassifier {

  private final BreakClass breakClass;

  protected BreakClassifier(BreakClass breakClass) {
    this.breakClass = breakClass;
  }

  public BreakClass breakClass() {
    return breakClass;
  }

  public ReconciliationBreakRecord classify(
      long runId,
      String domain,
      String entityName,
      String businessKey,
      String columnName,
      String legacyValue,
      String targetValue,
      String approvedDecisionId) {
    return new ReconciliationBreakRecord(
        null,
        runId,
        domain,
        breakClass,
        entityName,
        businessKey,
        columnName,
        legacyValue,
        targetValue,
        approvedDecisionId,
        Instant.now(),
        Instant.now());
  }
}
