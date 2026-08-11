package com.pcis.billing.domain.exception;

public class DuplicatePaymentException extends RuntimeException {

  private final String paymentRef;

  public DuplicatePaymentException(String paymentRef) {
    super("Payment already submitted: " + paymentRef);
    this.paymentRef = paymentRef;
  }

  public String getPaymentRef() {
    return paymentRef;
  }
}
