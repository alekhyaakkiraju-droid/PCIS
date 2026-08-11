package com.pcis.claims.exception;

/** Base class for payment authorization failures surfaced as RFC 9457 403 responses. */
public abstract class PaymentAuthorizationException extends RuntimeException {

  protected PaymentAuthorizationException(String message) {
    super(message);
  }
}
