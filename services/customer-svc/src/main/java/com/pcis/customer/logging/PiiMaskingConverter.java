package com.pcis.customer.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Logback ClassicConverter that redacts PII before log persistence.
 * Registered as conversionWord "piiMask" in logback-spring.xml.
 *
 * <p>Masked patterns:
 * <ul>
 *   <li>SSN: 000-00-0000 → ***-**-NNNN (last 4 preserved)</li>
 *   <li>Phone: 10+ consecutive digits → [PHONE]</li>
 *   <li>Email: user@domain.tld → ***@***.***</li>
 *   <li>Tax ID (FEIN): 00-0000000 → **-****NNN</li>
 * </ul>
 */
public class PiiMaskingConverter extends ClassicConverter {

  // SSN: DDD-DD-DDDD — capture last 4 digits to preserve in output
  private static final Pattern SSN_PATTERN =
      Pattern.compile("\\b(\\d{3})-(\\d{2})-(\\d{4})\\b");
  private static final String SSN_REPLACEMENT = "***-**-$3";

  // Phone: 10 or more consecutive digits (e.g. 8005551234 or longer)
  private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{10,}\\b");
  private static final String PHONE_REPLACEMENT = "[PHONE]";

  // Email: localpart@domain.tld
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
  private static final String EMAIL_REPLACEMENT = "***@***.***";

  // Tax ID / FEIN: DD-DDDDDDD — capture last 3 digits
  private static final Pattern TAX_ID_PATTERN =
      Pattern.compile("\\b(\\d{2})-(\\d{4})(\\d{3})\\b");
  private static final String TAX_ID_REPLACEMENT = "**-****$3";

  @Override
  public String convert(ILoggingEvent event) {
    String message = event.getFormattedMessage();
    return mask(message);
  }

  /** Applies all PII masking patterns. Package-private for direct unit testing. */
  static String mask(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    String result = SSN_PATTERN.matcher(input).replaceAll(SSN_REPLACEMENT);
    result = TAX_ID_PATTERN.matcher(result).replaceAll(TAX_ID_REPLACEMENT);
    result = EMAIL_PATTERN.matcher(result).replaceAll(EMAIL_REPLACEMENT);
    result = PHONE_PATTERN.matcher(result).replaceAll(PHONE_REPLACEMENT);
    return result;
  }
}
