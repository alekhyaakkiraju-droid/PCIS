package com.pcis.batch.claims.domain;

import java.math.BigDecimal;

public final class ClaimPaymentCalculator {

  private ClaimPaymentCalculator() {}

  public static BigDecimal paymentAmount(BigDecimal reserveAmt) {
    return reserveAmt;
  }

  public static boolean requiresReinsuranceRecovery(BigDecimal reserveAmt, BigDecimal cessionThreshold) {
    return reserveAmt.compareTo(cessionThreshold) > 0;
  }
}
