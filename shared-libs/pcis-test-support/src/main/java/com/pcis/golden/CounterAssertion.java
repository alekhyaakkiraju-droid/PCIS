package com.pcis.golden;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compares {@code RPT_RUN_LOG_T} counter fields and top-level run-log metadata against golden
 * expectations with exact integer equality.
 */
public final class CounterAssertion {

  private static final Set<String> COUNTER_COLUMNS =
      Set.of("REC_SELECTED", "REC_UPDATED", "REC_ERRORS", "ROWS_PROCESSED");

  private CounterAssertion() {}

  static void compare(
      GoldenDiff.Builder diffBuilder,
      Map<String, Object> expectedRunLog,
      Map<String, Object> actualRunLog,
      Map<String, Object> expectedRunLogRow,
      Map<String, Object> actualRunLogRow) {
    compareRunLogFields(diffBuilder, expectedRunLog, actualRunLog);
    compareRunLogTableRow(diffBuilder, expectedRunLogRow, actualRunLogRow);
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> findRunLogRow(GoldenArtifact artifact) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!"RPT_RUN_LOG_T".equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
      if (rows != null && !rows.isEmpty()) {
        return rows.get(0);
      }
    }
    return Map.of();
  }

  private static void compareRunLogFields(
      GoldenDiff.Builder diffBuilder,
      Map<String, Object> expected,
      Map<String, Object> actual) {
    Map<String, Object> key = Map.of("PROGRAM_NAME", expected.getOrDefault("programName", ""));

    compareField(diffBuilder, key, "status", expected.get("status"), actual.get("status"));
    compareField(
        diffBuilder,
        key,
        "rowsProcessed",
        expected.get("rowsProcessed"),
        actual.get("rowsProcessed"));
  }

  private static void compareRunLogTableRow(
      GoldenDiff.Builder diffBuilder,
      Map<String, Object> expectedRow,
      Map<String, Object> actualRow) {
    if (expectedRow.isEmpty() && actualRow.isEmpty()) {
      return;
    }
    Map<String, Object> key = new LinkedHashMap<>();
    key.put(
        "PROGRAM_NAME",
        String.valueOf(
            expectedRow.getOrDefault(
                "PROGRAM_NAME", actualRow.getOrDefault("PROGRAM_NAME", ""))));

    for (String col : COUNTER_COLUMNS) {
      if (!expectedRow.containsKey(col) && !actualRow.containsKey(col)) {
        continue;
      }
      compareField(diffBuilder, key, col, expectedRow.get(col), actualRow.get(col));
    }
  }

  private static void compareField(
      GoldenDiff.Builder diffBuilder,
      Map<String, Object> businessKey,
      String column,
      Object expected,
      Object actual) {
    String expectedStr = stringify(expected);
    String actualStr = stringify(actual);
    if (expectedStr.equals(actualStr)) {
      return;
    }
    DiffCategory category =
        "status".equals(column) || column.endsWith("_STATUS")
            ? DiffCategory.STATUS_MISMATCH
            : DiffCategory.COUNTER_MISMATCH;
    diffBuilder.add(
        new GoldenDiffEntry(
            "RPT_RUN_LOG_T",
            businessKey,
            column.toUpperCase(Locale.ROOT),
            expectedStr,
            actualStr,
            null,
            category));
  }

  private static String stringify(Object value) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value);
  }
}
