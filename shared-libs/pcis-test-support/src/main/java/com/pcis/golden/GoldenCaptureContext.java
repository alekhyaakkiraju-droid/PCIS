package com.pcis.golden;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Determinism context for golden capture: pinned clock, sequence rewriter, and
 * loaded normalization rules.
 */
public final class GoldenCaptureContext {

  public static final String NORMALIZED_TS = "NORMALIZED_TS";

  private final Clock clock;
  private final LocalDate referenceDate;
  private final NormalizationRules rules;
  private final NormalizationConfigValidator configValidator;
  private final SequenceOrdinalNormalizer sequenceNormalizer;

  private GoldenCaptureContext(
      Clock clock, LocalDate referenceDate, NormalizationRules rules) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.referenceDate = Objects.requireNonNull(referenceDate, "referenceDate");
    this.rules = Objects.requireNonNull(rules, "rules");
    this.configValidator = new NormalizationConfigValidator(rules);
    this.configValidator.validateRulesConsistency();
    this.sequenceNormalizer = new SequenceOrdinalNormalizer();
  }

  public static GoldenCaptureContext pinned(LocalDate referenceDate, NormalizationRules rules) {
    Instant instant = referenceDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
    return new GoldenCaptureContext(clock, referenceDate, rules);
  }

  public static GoldenCaptureContext pinned(String referenceDateIso, NormalizationRules rules) {
    return pinned(LocalDate.parse(referenceDateIso), rules);
  }

  public static GoldenCaptureContext fromRulesDefault(NormalizationRules rules) {
    return pinned(rules.defaultReferenceDate(), rules);
  }

  public Clock clock() {
    return clock;
  }

  public LocalDate referenceDate() {
    return referenceDate;
  }

  public Instant now() {
    return clock.instant();
  }

  public LocalDate currentDate() {
    return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  public NormalizationRules rules() {
    return rules;
  }

  public NormalizationConfigValidator configValidator() {
    return configValidator;
  }

  public SequenceOrdinalNormalizer sequenceNormalizer() {
    return sequenceNormalizer;
  }

  /**
   * Normalizes a cell value according to allow/deny rules. Monetary and status
   * columns are returned unchanged (as exact decimal/string forms).
   */
  public String normalizeValue(String columnName, String sqlType, Object rawValue) {
    String col = columnName.toUpperCase(java.util.Locale.ROOT);
    if (rules.isDenied(col)
        || (sqlType != null
            && rules.denyMonetaryTypes().contains(sqlType.toUpperCase(java.util.Locale.ROOT)))) {
      return formatExact(rawValue);
    }
    if (rules.isSurrogateColumn(col)) {
      return sequenceNormalizer.normalize(col, rawValue);
    }
    if (rules.isTimestampColumn(col)) {
      return NORMALIZED_TS;
    }
    return formatExact(rawValue);
  }

  private static String formatExact(Object rawValue) {
    if (rawValue == null) {
      return "";
    }
    if (rawValue instanceof java.math.BigDecimal bd) {
      // Never use floating point; preserve exact decimal text (incl. trailing zeros).
      return bd.toPlainString();
    }
    return String.valueOf(rawValue).trim();
  }
}
