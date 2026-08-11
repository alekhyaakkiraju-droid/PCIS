package com.pcis.batch.claims.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ClaimPaymentCalculator {

  private ClaimPaymentCalculator() {}

  public static BigDecimal outstandingAmount(BigDecimal approvedAmt, BigDecimal paidToDate) {
    return approvedAmt.subtract(paidToDate).setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal paymentAmount(BigDecimal approvedAmt, BigDecimal paidToDate) {
    return outstandingAmount(approvedAmt, paidToDate);
  }

  public static boolean requiresReinsuranceRecovery(
      BigDecimal paymentAmt, BigDecimal cessionThreshold) {
    return paymentAmt.compareTo(cessionThreshold) > 0;
  }
}
