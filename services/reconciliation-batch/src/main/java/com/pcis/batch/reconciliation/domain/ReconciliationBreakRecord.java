package com.pcis.batch.reconciliation.domain;

import java.time.Instant;

public record ReconciliationBreakRecord(
    Long breakId,
    long runId,
    String domain,
    BreakClass breakClass,
    String entityName,
    String businessKey,
    String columnName,
    String legacyValue,
    String targetValue,
    String approvedDecisionId,
    Instant firstSeenAt,
    Instant lastSeenAt) {

  public boolean unexplained() {
    return approvedDecisionId == null || approvedDecisionId.isBlank();
  }
}
