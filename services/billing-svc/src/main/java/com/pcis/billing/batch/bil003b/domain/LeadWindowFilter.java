package com.pcis.billing.batch.bil003b.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Lead-window eligibility matching BIL003B days_out <= leadDays semantics. */
public final class LeadWindowFilter {

  private LeadWindowFilter() {}

  public static long daysOut(LocalDate nextDueDate, LocalDate referenceDate) {
    return ChronoUnit.DAYS.between(referenceDate, nextDueDate);
  }

  public static boolean isEligible(long daysOut, int leadDays) {
    return daysOut <= leadDays;
  }

  public static boolean isEligible(LocalDate nextDueDate, LocalDate referenceDate, int leadDays) {
    return isEligible(daysOut(nextDueDate, referenceDate), leadDays);
  }
}
