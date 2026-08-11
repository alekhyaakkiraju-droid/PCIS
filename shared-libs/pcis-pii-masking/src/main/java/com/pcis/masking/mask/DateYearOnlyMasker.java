package com.pcis.masking.mask;

import com.pcis.masking.MaskingConstants;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the four-digit year from a date-of-birth value. */
public final class DateYearOnlyMasker implements ValueMasker {

  private static final Pattern YEAR_PREFIX = Pattern.compile("(\\d{4})");

  @Override
  public String mask(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    if (YEAR_PREFIX.matcher(value).matches() && value.length() == 4) {
      return value;
    }
    try {
      LocalDate parsed = LocalDate.parse(value);
      return String.valueOf(parsed.getYear());
    } catch (DateTimeParseException ignored) {
      Matcher matcher = YEAR_PREFIX.matcher(value);
      if (matcher.find()) {
        return matcher.group(1);
      }
      return MaskingConstants.FULL_REDACT_TOKEN;
    }
  }
}
