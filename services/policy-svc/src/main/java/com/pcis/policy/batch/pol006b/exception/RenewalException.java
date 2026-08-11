package com.pcis.policy.batch.pol006b.exception;

public class RenewalException extends RuntimeException {

  private final String policyNumber;
  private final String reasonCode;

  public RenewalException(String policyNumber, String reasonCode, String message) {
    super(message);
    this.policyNumber = policyNumber;
    this.reasonCode = reasonCode;
  }

  public String getPolicyNumber() {
    return policyNumber;
  }

  public String getReasonCode() {
    return reasonCode;
  }
}
