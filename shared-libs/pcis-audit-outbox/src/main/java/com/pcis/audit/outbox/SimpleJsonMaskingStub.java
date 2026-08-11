package com.pcis.audit.outbox;

import java.util.regex.Pattern;

/**
 * Minimal JSON-field masking stub until {@code pcis-pii-masking} is available.
 *
 * <p>Redacts common PII patterns in {@code old_value} and {@code new_value} strings.
 */
public class SimpleJsonMaskingStub implements AuditPayloadMasker {

  private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
  private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@([\\w.-]+\\.[A-Za-z]{2,})\\b");
  private static final Pattern PHONE = Pattern.compile("\\b\\d{3}-\\d{3}-(\\d{4})\\b");

  @Override
  public String mask(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String masked = SSN.matcher(value).replaceAll("***-**-****");
    masked = EMAIL.matcher(masked).replaceAll("***@$1");
    masked = PHONE.matcher(masked).replaceAll("***-***-$1");
    return masked;
  }
}
