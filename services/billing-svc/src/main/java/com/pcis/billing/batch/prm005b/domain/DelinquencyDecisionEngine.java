package com.pcis.billing.batch.prm005b.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class DelinquencyDecisionEngine {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private DelinquencyDecisionEngine() {}

  public static Optional<StatusTransition> evaluate(
      BigDecimal amtDue,
      BigDecimal amtPaid,
      long daysPastDue,
      int graceDays,
      String currentStatus) {
    if (amtDue == null) {
      return Optional.empty();
    }
    String status = currentStatus == null ? "" : currentStatus.trim();
    BigDecimal paid = amtPaid == null ? ZERO : amtPaid.setScale(2, RoundingMode.HALF_UP);
    BigDecimal due = amtDue.setScale(2, RoundingMode.HALF_UP);

    if (paid.compareTo(due) >= 0) {
      if ("P".equals(status)) {
        return Optional.empty();
      }
      return Optional.of(new StatusTransition(status, "P", daysPastDue, false));
    }
    if ("O".equals(status) && daysPastDue > graceDays) {
      return Optional.of(new StatusTransition(status, "L", daysPastDue, true));
    }
    return Optional.empty();
  }
}
