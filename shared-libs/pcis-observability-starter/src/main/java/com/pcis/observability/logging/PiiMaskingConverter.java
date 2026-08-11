package com.pcis.observability.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback {@link ClassicConverter} that redacts Restricted-tier PII (SSN/tax ID, phone, email)
 * from log messages before emission.
 *
 * <p>Masking formats:
 * <ul>
 *   <li>SSN / tax ID ({@code ddd-dd-dddd} or 9 consecutive digits): {@code ***-**-****}</li>
 *   <li>Phone (10+ digit sequences / common NA formats): {@code ****-last4}</li>
 *   <li>Email: {@code first2***@domain}</li>
 * </ul>
 */
public class PiiMaskingConverter extends ClassicConverter {

  private static final Pattern SSN_DASHED =
      Pattern.compile("(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)");
  /** Nine-digit tax ids; avoid matching hex fragments inside UUIDs. */
  private static final Pattern SSN_PLAIN =
      Pattern.compile("(?<![A-Za-z0-9])\\d{9}(?![A-Za-z0-9])");
  private static final Pattern PHONE =
      Pattern.compile(
          "(?<!\\d)(?:\\+?1[-.\\s]?)?(?:\\(?\\d{3}\\)?[-.\\s]?)\\d{3}[-.\\s]?\\d{4}(?!\\d)");
  private static final Pattern EMAIL =
      Pattern.compile(
          "(?i)\\b([A-Za-z0-9._%+-]{1,64})@([A-Za-z0-9.-]+\\.[A-Za-z]{2,63})\\b");

  @Override
  public String convert(ILoggingEvent event) {
    if (event == null) {
      return "";
    }
    String message = event.getFormattedMessage();
    if (message == null) {
      return "";
    }
    try {
      return maskPii(message);
    } catch (RuntimeException ex) {
      // Never break the logging pipeline
      try {
        addWarn("PII masking failed; emitting original message: " + ex.getMessage());
      } catch (RuntimeException ignored) {
        // Context may be unavailable in unit tests
      }
      return message;
    }
  }

  /**
   * Applies all PII masking rules to the given text. Public for unit tests and reuse from
   * throwable / JSON providers.
   */
  public static String maskPii(String input) {
    if (input == null || input.isEmpty()) {
      return input == null ? "" : input;
    }
    String masked = SSN_DASHED.matcher(input).replaceAll("***-**-****");
    masked = SSN_PLAIN.matcher(masked).replaceAll("***-**-****");
    masked = maskPhones(masked);
    masked = maskEmails(masked);
    return masked;
  }

  private static String maskPhones(String input) {
    Matcher matcher = PHONE.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String digits = matcher.group().replaceAll("\\D", "");
      if (digits.length() < 10) {
        matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
        continue;
      }
      String last4 = digits.substring(digits.length() - 4);
      matcher.appendReplacement(sb, Matcher.quoteReplacement("****-" + last4));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static String maskEmails(String input) {
    Matcher matcher = EMAIL.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String local = matcher.group(1);
      String domain = matcher.group(2);
      String prefix = local.length() <= 2 ? local : local.substring(0, 2);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + "***@" + domain));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
