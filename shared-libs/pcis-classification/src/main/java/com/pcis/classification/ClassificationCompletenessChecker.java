package com.pcis.classification;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Fail-closed gate comparing PostgreSQL schema tables to the classification registry. */
public final class ClassificationCompletenessChecker {

  private static final Set<MaskStrategy> ALLOWED_STRATEGIES = EnumSet.allOf(MaskStrategy.class);

  static final Set<String> EXCLUDED_SYSTEM_TABLES =
      Set.of("FLYWAY_SCHEMA_HISTORY", "DATA_CLASSIFICATION", "DATA_CLASSIFICATION_TIER");

  private ClassificationCompletenessChecker() {}

  public static CompletenessReport checkCompleteness(
      Set<String> schemaTables, DataClassificationRegistry registry) {
    if (schemaTables == null) {
      throw new ClassificationRegistryException("Schema table set must not be null");
    }
    if (registry == null) {
      throw new ClassificationRegistryException("Classification registry must not be null");
    }

    Set<String> classifiedTables = registry.getClassifiedEntityNames();
    List<String> unclassified =
        schemaTables.stream()
            .filter(table -> !classifiedTables.contains(normalizeTable(table)))
            .sorted()
            .toList();

    List<String> missingStrategy = new ArrayList<>();
    List<String> invalidStrategy = new ArrayList<>();

    for (ClassificationEntry entry : registry.getAllEntries()) {
      if (entry.tier() == DataTier.RESTRICTED
          && entry.pii()
          && (entry.maskStrategy() == null || entry.maskStrategy() == MaskStrategy.NONE)) {
        missingStrategy.add(entry.entityName() + "." + entry.columnName());
      }
      if (entry.maskStrategy() != null && !ALLOWED_STRATEGIES.contains(entry.maskStrategy())) {
        invalidStrategy.add(
            entry.entityName() + "." + entry.columnName() + " -> " + entry.maskStrategy());
      }
    }

    missingStrategy.sort(String::compareTo);
    invalidStrategy.sort(String::compareTo);

    boolean passed = unclassified.isEmpty() && missingStrategy.isEmpty() && invalidStrategy.isEmpty();
    return new CompletenessReport(
        schemaTables.size(),
        classifiedTables.size(),
        List.copyOf(unclassified),
        List.copyOf(missingStrategy),
        List.copyOf(invalidStrategy),
        passed);
  }

  public static String formatFailureMessage(CompletenessReport report) {
    StringBuilder message = new StringBuilder("Classification completeness gate failed:");
    if (!report.unclassifiedTables().isEmpty()) {
      message.append("\nUnclassified tables: ").append(report.unclassifiedTables());
    }
    if (!report.restrictedColumnsWithoutStrategy().isEmpty()) {
      message
          .append("\nRestricted PII columns without mask_strategy: ")
          .append(report.restrictedColumnsWithoutStrategy());
    }
    if (!report.invalidMaskStrategies().isEmpty()) {
      message.append("\nInvalid mask_strategy values: ").append(report.invalidMaskStrategies());
    }
    return message.toString();
  }

  static String normalizeTable(String tableName) {
    return tableName == null ? "" : tableName.trim().toUpperCase();
  }
}
