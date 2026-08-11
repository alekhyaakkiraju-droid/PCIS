package com.pcis.masking.mask;

import java.util.regex.Pattern;

/** Keeps the last four digits of a phone number. */
public final class PhoneLastFourMasker implements ValueMasker {

  private static final Pattern NON_DIGIT = Pattern.compile("\\D");

  @Override
  public String mask(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    String digits = NON_DIGIT.matcher(value).replaceAll("");
    if (digits.isEmpty()) {
      return value;
    }
    if (digits.length() <= 4) {
      return digits;
    }
    return digits.substring(digits.length() - 4);
  }
}
