package com.pcis.claims.reconciliation;

import java.util.List;

public record ReconciliationReport(
    long totalLegacyRecords,
    long totalTargetRecords,
    long matchedRecords,
    List<ReconciliationBreak> breaks,
    OverallStatus overallStatus) {

  public enum OverallStatus {
    PASS,
    FAIL
  }

  public static ReconciliationReport pass(long legacyCount, long targetCount, long matched) {
    return new ReconciliationReport(legacyCount, targetCount, matched, List.of(), OverallStatus.PASS);
  }

  public static ReconciliationReport fail(
      long legacyCount, long targetCount, long matched, List<ReconciliationBreak> breaks) {
    return new ReconciliationReport(legacyCount, targetCount, matched, breaks, OverallStatus.FAIL);
  }
}
