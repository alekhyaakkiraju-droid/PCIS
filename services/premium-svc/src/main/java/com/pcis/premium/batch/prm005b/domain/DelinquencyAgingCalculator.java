package com.pcis.premium.batch.prm005b.domain;

import java.math.BigDecimal;

public final class DelinquencyAgingCalculator {

  private DelinquencyAgingCalculator() {}

  public static String computeStatus(
      BigDecimal amtDue, BigDecimal amtPaid, int daysPastDue, int graceDays) {
    BigDecimal paid = amtPaid != null ? amtPaid : BigDecimal.ZERO;
    if (paid.compareTo(amtDue) >= 0) {
      return "P";
    }
    if (daysPastDue > graceDays) {
      return "L";
    }
    return "D";
  }

  public static boolean statusChanged(String currentStatus, String newStatus) {
    return currentStatus == null || !currentStatus.equals(newStatus);
  }
}
