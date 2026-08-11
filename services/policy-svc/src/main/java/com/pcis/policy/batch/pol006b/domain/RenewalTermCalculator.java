package com.pcis.policy.batch.pol006b.domain;

import java.time.LocalDate;

public final class RenewalTermCalculator {

  private RenewalTermCalculator() {}

  /** New effective date is the day after the expiring term ends. */
  public static LocalDate newEffectiveDate(LocalDate expiringExpDate) {
    return expiringExpDate.plusDays(1);
  }

  /** Preserve one-year term length from the prior policy period. */
  public static LocalDate newExpirationDate(LocalDate expiringEffDate, LocalDate expiringExpDate) {
    long termDays = java.time.temporal.ChronoUnit.DAYS.between(expiringEffDate, expiringExpDate);
    if (termDays <= 0) {
      termDays = 365;
    }
    return newEffectiveDate(expiringExpDate).plusDays(termDays);
  }

  /**
   * Derive a renewal policy number within VARCHAR(12) while preserving trailing uniqueness
   * from the source policy number (insert {@code R} before the final character).
   */
  public static String renewalPolicyNumber(String sourcePolNbr) {
    if (sourcePolNbr == null || sourcePolNbr.isEmpty()) {
      throw new IllegalArgumentException("policy number required");
    }
    if (sourcePolNbr.length() == 1) {
      return sourcePolNbr + "R";
    }
    int insertAt = Math.min(sourcePolNbr.length() - 1, 10);
    return sourcePolNbr.substring(0, insertAt)
        + "R"
        + sourcePolNbr.substring(insertAt + 1);
  }

  public static String deriveCoverageId(String newPolNbr, String oldCoverageId) {
    String suffix =
        oldCoverageId.length() > 4
            ? oldCoverageId.substring(oldCoverageId.length() - 4)
            : oldCoverageId;
    String candidate = newPolNbr + suffix;
    return candidate.length() <= 14 ? candidate : candidate.substring(0, 14);
  }
}
