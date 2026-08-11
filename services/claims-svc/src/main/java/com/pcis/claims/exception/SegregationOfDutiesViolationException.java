package com.pcis.claims.exception;

/** Raised when payment initiator is the same principal as the approver (SoD violation). */
public class SegregationOfDutiesViolationException extends PaymentAuthorizationException {

  public SegregationOfDutiesViolationException() {
    super("Approver and payment initiator must be distinct principals");
  }
}
