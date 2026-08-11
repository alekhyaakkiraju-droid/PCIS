package com.pcis.claims.exception;

/** Raised when no active approval exists for the requested reserve. */
public class ApprovalRequiredException extends PaymentAuthorizationException {

  public ApprovalRequiredException(Long reserveId) {
    super("No active approval exists for reserve: " + reserveId);
  }
}
