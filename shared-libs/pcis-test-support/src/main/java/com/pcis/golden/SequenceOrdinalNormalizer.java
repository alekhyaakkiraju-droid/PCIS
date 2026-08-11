package com.pcis.golden;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rewrites IDENTITY/SEQUENCE surrogate values to stable ordinal placeholders
 * ({@code SEQ_001}, {@code SEQ_002}, …) in first-seen order after business-key
 * sorting.
 */
public final class SequenceOrdinalNormalizer {

  private final Map<String, Map<String, String>> byColumn = new LinkedHashMap<>();
  private final Map<String, Integer> counters = new LinkedHashMap<>();

  public String normalize(String columnName, Object rawValue) {
    if (rawValue == null) {
      return "";
    }
    String text = String.valueOf(rawValue).trim();
    if (text.isEmpty() || "NULL".equalsIgnoreCase(text) || "\\N".equals(text)) {
      return "";
    }
    String col = columnName.toUpperCase(Locale.ROOT);
    Map<String, String> mapping =
        byColumn.computeIfAbsent(col, ignored -> new LinkedHashMap<>());
    return mapping.computeIfAbsent(
        text,
        key -> {
          int next = counters.merge(col, 1, Integer::sum);
          return String.format(Locale.ROOT, "SEQ_%03d", next);
        });
  }

  public void reset() {
    byColumn.clear();
    counters.clear();
  }
}
