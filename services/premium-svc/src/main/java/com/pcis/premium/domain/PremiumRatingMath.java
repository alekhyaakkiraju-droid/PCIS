package com.pcis.premium.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Pure BigDecimal helpers for premium rating (HALF_UP per COBOL ROUNDED mapping). */
public final class PremiumRatingMath {

  private PremiumRatingMath() {}

  public static BigDecimal combineFactors(List<BigDecimal> factorValues) {
    if (factorValues == null || factorValues.isEmpty()) {
      return BigDecimal.ONE;
    }
    return factorValues.stream().reduce(BigDecimal.ONE, BigDecimal::multiply);
  }

  public static BigDecimal multiplyAndRound(
      BigDecimal multiplicand, BigDecimal multiplier, int scale) {
    return multiplicand.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);
  }
}
