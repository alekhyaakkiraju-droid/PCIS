package com.pcis.claims.exception;

import java.math.BigDecimal;

/** Raised when payment amount exceeds the reserve outstanding balance. */
public class InsufficientReserveException extends PaymentAuthorizationException {

  private final BigDecimal outstanding;

  public InsufficientReserveException(BigDecimal outstanding) {
    super("Payment amount exceeds reserve outstanding balance: " + outstanding.toPlainString());
    this.outstanding = outstanding;
  }

  public BigDecimal getOutstanding() {
    return outstanding;
  }
}
