package com.pcis.classification;

import java.util.List;

/** Result of comparing Flyway schema tables against the classification registry. */
public record CompletenessReport(
    int totalSchemaTables,
    int totalClassifiedTables,
    List<String> unclassifiedTables,
    List<String> restrictedColumnsWithoutStrategy,
    List<String> invalidMaskStrategies,
    boolean passed) {

  public static CompletenessReport pass(int schemaTables, int classifiedTables) {
    return new CompletenessReport(schemaTables, classifiedTables, List.of(), List.of(), List.of(), true);
  }
}
