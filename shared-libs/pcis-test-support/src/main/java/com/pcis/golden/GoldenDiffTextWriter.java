package com.pcis.golden;

import java.util.Map;

/** Human-readable text diff for developer review. */
public final class GoldenDiffTextWriter {

  private GoldenDiffTextWriter() {}

  public static String render(GoldenDiff diff) {
    if (diff.isMatch()) {
      return "PASS: " + diff.getScenarioId() + " matches golden\n";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("FAIL: ")
        .append(diff.getScenarioId())
        .append(" — ")
        .append(diff.getTotalDiffCount())
        .append(" difference(s)");
    if (diff.isTruncated()) {
      sb.append(" (showing first ").append(diff.getEntries().size()).append(")");
    }
    sb.append('\n');
    sb.append(String.format("%-28s %-24s %-18s %-14s %-14s %-10s%n",
        "TABLE", "BUSINESS_KEY", "COLUMN", "EXPECTED", "ACTUAL", "DELTA"));
    sb.append("-".repeat(108)).append('\n');
    for (GoldenDiffEntry entry : diff.getEntries()) {
      sb.append(
          String.format(
              "%-28s %-24s %-18s %-14s %-14s %-10s%n",
              truncate(entry.getTable(), 28),
              truncate(formatKey(entry.getBusinessKey()), 24),
              truncate(entry.getColumn(), 18),
              truncate(entry.getExpectedValue(), 14),
              truncate(entry.getActualValue(), 14),
              truncate(nullToDash(entry.getSignedDelta()), 10)));
      sb.append("  [").append(entry.getCategory()).append("]\n");
    }
    return sb.toString();
  }

  private static String formatKey(Map<String, Object> key) {
    if (key == null || key.isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> e : key.entrySet()) {
      if (!first) {
        sb.append(',');
      }
      sb.append(e.getKey()).append('=').append(e.getValue());
      first = false;
    }
    sb.append('}');
    return sb.toString();
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max - 3) + "...";
  }

  private static String nullToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
