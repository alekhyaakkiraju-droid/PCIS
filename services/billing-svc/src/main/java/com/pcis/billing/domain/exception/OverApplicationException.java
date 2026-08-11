package com.pcis.billing.domain.exception;

import java.math.BigDecimal;

public class OverApplicationException extends RuntimeException {

  private final BigDecimal maxApplicableAmount;

  public OverApplicationException(BigDecimal maxApplicableAmount) {
    super("Payment amount exceeds outstanding balance of " + maxApplicableAmount.toPlainString());
    this.maxApplicableAmount = maxApplicableAmount;
  }

  public BigDecimal getMaxApplicableAmount() {
    return maxApplicableAmount;
  }
}
