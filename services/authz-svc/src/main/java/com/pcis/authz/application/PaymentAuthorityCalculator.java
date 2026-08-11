package com.pcis.authz.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** BigDecimal monetary arithmetic for cumulative payment authority (BR-01 / P-B01). */
final class PaymentAuthorityCalculator {

  static final int MONETARY_SCALE = 2;
  static final RoundingMode MONETARY_ROUNDING = RoundingMode.HALF_UP;

  private PaymentAuthorityCalculator() {}

  static BigDecimal cumulativePayout(BigDecimal paidToDate, BigDecimal requestedAmount) {
    BigDecimal paid = normalize(paidToDate);
    BigDecimal requested = normalize(requestedAmount);
    return paid.add(requested).setScale(MONETARY_SCALE, MONETARY_ROUNDING);
  }

  static boolean exceedsAuthorityLimit(BigDecimal cumulativePayout, BigDecimal authorityLimit) {
    return cumulativePayout.compareTo(normalize(authorityLimit)) > 0;
  }

  static BigDecimal normalize(BigDecimal amount) {
    if (amount == null) {
      return BigDecimal.ZERO.setScale(MONETARY_SCALE, MONETARY_ROUNDING);
    }
    return amount.setScale(MONETARY_SCALE, MONETARY_ROUNDING);
  }
}
