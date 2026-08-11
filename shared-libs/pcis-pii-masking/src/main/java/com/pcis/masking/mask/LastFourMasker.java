package com.pcis.masking.mask;

import java.util.regex.Pattern;

/** Keeps the last four alphanumeric characters (tax IDs, account numbers). */
public final class LastFourMasker implements ValueMasker {

  private static final Pattern ALNUM = Pattern.compile("[^A-Za-z0-9]");

  @Override
  public String mask(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    String normalized = ALNUM.matcher(value).replaceAll("");
    if (normalized.isEmpty()) {
      return value;
    }
    if (normalized.length() <= 4) {
      return normalized;
    }
    return normalized.substring(normalized.length() - 4);
  }
}
