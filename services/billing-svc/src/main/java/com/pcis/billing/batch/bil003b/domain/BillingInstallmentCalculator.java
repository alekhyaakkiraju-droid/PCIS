package com.pcis.billing.batch.bil003b.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class BillingInstallmentCalculator {

  private BillingInstallmentCalculator() {}

  public static BigDecimal installmentAmount(BigDecimal premAnnual, int installmentCnt) {
    return premAnnual.divide(BigDecimal.valueOf(installmentCnt), 2, RoundingMode.HALF_UP);
  }

  public static LocalDate nextDueDate(LocalDate lastDueDate, String billFreq, LocalDate referenceDate) {
    if (lastDueDate == null) {
      return referenceDate;
    }
    return switch (billFreq) {
      case "M" -> lastDueDate.plusMonths(1);
      case "Q" -> lastDueDate.plusMonths(3);
      case "S" -> lastDueDate.plusMonths(6);
      case "A" -> lastDueDate.plusYears(1);
      default -> lastDueDate.plusMonths(1);
    };
  }

  public static long daysOut(LocalDate nextDueDate, LocalDate referenceDate) {
    return ChronoUnit.DAYS.between(referenceDate, nextDueDate);
  }

  public static boolean withinLeadWindow(long daysOut, int leadDays) {
    return daysOut <= leadDays;
  }
}
