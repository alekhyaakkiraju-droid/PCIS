package com.pcis.billing.batch.bil003b.exception;

/** Structured business-rule violation for skippable billing generation items. */
public class BusinessRuleException extends RuntimeException {

  private final String polNbr;
  private final String reasonCode;

  public BusinessRuleException(String polNbr, String reasonCode, String message) {
    super(message);
    this.polNbr = polNbr;
    this.reasonCode = reasonCode;
  }

  public String getPolNbr() {
    return polNbr;
  }

  public String getReasonCode() {
    return reasonCode;
  }
}
