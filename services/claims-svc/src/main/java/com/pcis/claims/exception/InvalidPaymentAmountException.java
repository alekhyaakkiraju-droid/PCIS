package com.pcis.claims.exception;

/** Raised when a payment amount is zero or negative. */
public class InvalidPaymentAmountException extends PaymentAuthorizationException {

  public InvalidPaymentAmountException() {
    super("Payment amount must be greater than zero");
  }
}
