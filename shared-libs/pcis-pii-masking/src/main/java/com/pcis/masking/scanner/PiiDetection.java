package com.pcis.masking.scanner;

/** A single unmasked PII detection from a database row or log line. */
public record PiiDetection(
    String source,
    String location,
    String rowId,
    PiiPattern patternType,
    String snippet) {

  public static String snippetFor(String matchedValue) {
    if (matchedValue == null || matchedValue.isEmpty()) {
      return "";
    }
    if (matchedValue.length() <= 4) {
      return matchedValue;
    }
    return matchedValue.substring(0, 2) + "..." + matchedValue.substring(matchedValue.length() - 2);
  }
}
