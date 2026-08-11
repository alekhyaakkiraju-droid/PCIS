package com.pcis.masking.scanner;

import java.util.regex.Pattern;

/** Detectable unmasked PII patterns aligned with Logback masking rules. */
public enum PiiPattern {
  SSN_DASHED(
      "(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)", "SSN (dashed)", Pattern.CASE_INSENSITIVE),
  SSN_NUMERIC("(?<![A-Za-z0-9])\\d{9}(?![A-Za-z0-9])", "SSN (numeric)", 0),
  EMAIL(
      "(?i)\\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}\\b",
      "Email address",
      Pattern.CASE_INSENSITIVE),
  PHONE(
      "(?<!\\d)(?:\\+?1[-.\\s]?)?(?:\\(?\\d{3}\\)?[-.\\s]?)\\d{3}[-.\\s]?\\d{4}(?!\\d)",
      "Phone number",
      0),
  VIN(
      "\\b[A-HJ-NPR-Z0-9]{17}\\b",
      "Vehicle identification number",
      Pattern.CASE_INSENSITIVE);

  private final Pattern pattern;
  private final String label;

  PiiPattern(String regex, String label, int flags) {
    this.pattern = Pattern.compile(regex, flags);
    this.label = label;
  }

  public Pattern pattern() {
    return pattern;
  }

  public String label() {
    return label;
  }
}
