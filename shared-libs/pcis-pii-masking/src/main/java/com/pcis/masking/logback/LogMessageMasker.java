package com.pcis.masking.logback;

import com.pcis.classification.ClassificationEntry;
import com.pcis.classification.DataClassificationRegistry;
import com.pcis.classification.DataTier;
import com.pcis.classification.MaskStrategy;
import com.pcis.masking.MaskingService;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scans log messages for Restricted-tier PII and masks via the classification registry. */
public final class LogMessageMasker {

  private static final List<PatternRule> DEFAULT_RULES =
      List.of(
          new PatternRule(
              Pattern.compile("(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)"), "CUSTOMER_T", "TAX_ID"),
          new PatternRule(
              Pattern.compile("(?<![A-Za-z0-9])\\d{9}(?![A-Za-z0-9])"), "CUSTOMER_T", "TAX_ID"),
          new PatternRule(
              Pattern.compile(
                  "(?i)\\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}\\b"),
              "CUSTOMER_T",
              "EMAIL"),
          new PatternRule(
              Pattern.compile(
                  "(?<!\\d)(?:\\+?1[-.\\s]?)?(?:\\(?\\d{3}\\)?[-.\\s]?)\\d{3}[-.\\s]?\\d{4}(?!\\d)"),
              "CUSTOMER_T",
              "PHONE"),
          new PatternRule(
              Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b"), "CUSTOMER_T", "DOB"));

  private final MaskingService maskingService;
  private final DataClassificationRegistry registry;
  private final List<PatternRule> rules;

  public LogMessageMasker(MaskingService maskingService, DataClassificationRegistry registry) {
    this(maskingService, registry, DEFAULT_RULES);
  }

  LogMessageMasker(
      MaskingService maskingService,
      DataClassificationRegistry registry,
      List<PatternRule> rules) {
    this.maskingService = maskingService;
    this.registry = registry;
    this.rules = List.copyOf(rules);
  }

  public static LogMessageMasker withDefaultRegistry() {
    DataClassificationRegistry registry = loadDefaultRegistry();
    return new LogMessageMasker(new MaskingService(registry), registry);
  }

  public String mask(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    String masked = input;
    for (PatternRule rule : rules) {
      masked = applyRule(masked, rule);
    }
    return masked;
  }

  public String maskMdcValue(String key, String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return registry.findEntry("CUSTOMER_T", key)
        .filter(entry -> entry.tier() == DataTier.RESTRICTED)
        .map(entry -> maskingService.mask(entry.entityName(), entry.columnName(), value))
        .orElseGet(() -> mask(value));
  }

  private String applyRule(String input, PatternRule rule) {
    Matcher matcher = rule.pattern().matcher(input);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String original = matcher.group();
      String masked = maskingService.mask(rule.entityName(), rule.columnName(), original);
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(masked));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private static DataClassificationRegistry loadDefaultRegistry() {
    DataClassificationRegistry registry = new DataClassificationRegistry();
    registry.replaceAll(
        List.of(
            new ClassificationEntry(
                "CUSTOMER_T", "TAX_ID", DataTier.RESTRICTED, MaskStrategy.LAST_FOUR, 2555, true, null, "log fallback"),
            new ClassificationEntry(
                "CUSTOMER_T", "EMAIL", DataTier.RESTRICTED, MaskStrategy.EMAIL_DOMAIN_ONLY, 2555, true, null, "log fallback"),
            new ClassificationEntry(
                "CUSTOMER_T", "PHONE", DataTier.RESTRICTED, MaskStrategy.PHONE_LAST_FOUR, 2555, true, null, "log fallback"),
            new ClassificationEntry(
                "CUSTOMER_T", "DOB", DataTier.RESTRICTED, MaskStrategy.DATE_YEAR_ONLY, 2555, true, null, "log fallback")));
    return registry;
  }

  record PatternRule(Pattern pattern, String entityName, String columnName) {}
}
