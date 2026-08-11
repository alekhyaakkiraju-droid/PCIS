package com.pcis.billing.batch.cmm001b.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CommissionCalculator {

  private CommissionCalculator() {}

  public static BigDecimal commissionAmount(BigDecimal paidAmount, BigDecimal rate) {
    return paidAmount
        .multiply(rate)
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }
}
