package com.pcis.batch.reconciliation.domain;

import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationRunSummary(
    long runId,
    String domain,
    LocalDate businessDate,
    Instant startedAt,
    Instant completedAt,
    int entityCount,
    long rowsCompared,
    long breakCount,
    long unexplainedBreakCount,
    GateVerdict gateVerdict,
    int consecutiveCleanDays) {

  public enum GateVerdict {
    PASS,
    FAIL
  }
}
