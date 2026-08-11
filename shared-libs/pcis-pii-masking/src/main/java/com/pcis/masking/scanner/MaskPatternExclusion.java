package com.pcis.masking.scanner;

import java.util.regex.Pattern;

/** Recognizes common mask output formats so the scanner does not false-positive on masked values. */
final class MaskPatternExclusion {

  private static final Pattern MASKED_PREFIX = Pattern.compile("^\\*{2,3}.*");
  private static final Pattern REDACTED = Pattern.compile("\\[REDACTED\\]", Pattern.CASE_INSENSITIVE);
  private static final Pattern XXX_SSN = Pattern.compile("XXX-XX-\\d{4}");
  private static final Pattern STAR_SSN = Pattern.compile("\\*{3}-\\*{2}-\\d{4}");
  private static final Pattern MASKED_EMAIL = Pattern.compile("\\*{2,3}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}");
  private static final Pattern DOMAIN_ONLY_EMAIL =
      Pattern.compile("(?i)\\*{2,3}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}");

  private MaskPatternExclusion() {}

  static boolean isMaskedValue(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    return MASKED_PREFIX.matcher(value).matches()
        || REDACTED.matcher(value).find()
        || XXX_SSN.matcher(value).find()
        || STAR_SSN.matcher(value).find()
        || MASKED_EMAIL.matcher(value).find()
        || DOMAIN_ONLY_EMAIL.matcher(value).find()
        || value.contains("***");
  }
}
