package com.pcis.billing.batch.bil003b.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @deprecated Use {@link InstallmentCalculator}, {@link FrequencyIntervalMapper}, and {@link
 *     LeadWindowFilter} directly.
 */
@Deprecated
public final class BillingInstallmentCalculator {

  private BillingInstallmentCalculator() {}

  public static BigDecimal installmentAmount(BigDecimal premAnnual, int installmentCnt) {
    return InstallmentCalculator.calculate(premAnnual, installmentCnt, 1);
  }

  public static LocalDate nextDueDate(
      LocalDate lastDueDate, String billFreq, LocalDate referenceDate) {
    return FrequencyIntervalMapper.nextDueDate(lastDueDate, billFreq, referenceDate);
  }

  public static long daysOut(LocalDate nextDueDate, LocalDate referenceDate) {
    return LeadWindowFilter.daysOut(nextDueDate, referenceDate);
  }

  public static boolean withinLeadWindow(long daysOut, int leadDays) {
    return LeadWindowFilter.isEligible(daysOut, leadDays);
  }
}
