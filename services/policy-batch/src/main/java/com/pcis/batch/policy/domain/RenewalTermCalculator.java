package com.pcis.batch.policy.domain;

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

  /** Derive a deterministic renewal policy number within VARCHAR(12). */
  public static String renewalPolicyNumber(String sourcePolNbr) {
    String suffix = "R";
    int maxBase = 12 - suffix.length();
    String base =
        sourcePolNbr.length() <= maxBase ? sourcePolNbr : sourcePolNbr.substring(0, maxBase);
    return base + suffix;
  }
}
