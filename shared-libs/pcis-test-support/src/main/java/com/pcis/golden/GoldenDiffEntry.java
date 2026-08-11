package com.pcis.golden;

import java.util.LinkedHashMap;
import java.util.Map;

/** A single row/column divergence between golden expected and actual output. */
public final class GoldenDiffEntry {

  private final String table;
  private final Map<String, Object> businessKey;
  private final String column;
  private final String expectedValue;
  private final String actualValue;
  private final String signedDelta;
  private final DiffCategory category;

  public GoldenDiffEntry(
      String table,
      Map<String, Object> businessKey,
      String column,
      String expectedValue,
      String actualValue,
      String signedDelta,
      DiffCategory category) {
    this.table = table;
    this.businessKey = Map.copyOf(new LinkedHashMap<>(businessKey));
    this.column = column;
    this.expectedValue = expectedValue;
    this.actualValue = actualValue;
    this.signedDelta = signedDelta;
    this.category = category;
  }

  public String getTable() {
    return table;
  }

  public Map<String, Object> getBusinessKey() {
    return businessKey;
  }

  public String getColumn() {
    return column;
  }

  public String getExpectedValue() {
    return expectedValue;
  }

  public String getActualValue() {
    return actualValue;
  }

  public String getSignedDelta() {
    return signedDelta;
  }

  public DiffCategory getCategory() {
    return category;
  }
}
