package com.pcis.batch.reconciliation.domain;

import java.util.ArrayList;
import java.util.List;

public final class DomainComparisonResult {

  private final String domain;
  private final int entityCount;
  private long rowsCompared;
  private final List<ReconciliationBreakRecord> breaks = new ArrayList<>();

  public DomainComparisonResult(String domain, int entityCount) {
    this.domain = domain;
    this.entityCount = entityCount;
  }

  public String domain() {
    return domain;
  }

  public int entityCount() {
    return entityCount;
  }

  public long rowsCompared() {
    return rowsCompared;
  }

  public void addRowsCompared(long count) {
    rowsCompared += count;
  }

  public List<ReconciliationBreakRecord> breaks() {
    return List.copyOf(breaks);
  }

  public void addBreak(ReconciliationBreakRecord breakRecord) {
    breaks.add(breakRecord);
  }

  public long unexplainedBreakCount() {
    return breaks.stream().filter(ReconciliationBreakRecord::unexplained).count();
  }
}
