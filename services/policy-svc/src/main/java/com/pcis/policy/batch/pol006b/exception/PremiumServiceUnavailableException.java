package com.pcis.policy.batch.pol006b.exception;

public class PremiumServiceUnavailableException extends RuntimeException {

  private final String policyNumber;

  public PremiumServiceUnavailableException(String policyNumber, Throwable cause) {
    super("premium-svc unavailable for policy " + policyNumber, cause);
    this.policyNumber = policyNumber;
  }

  public String getPolicyNumber() {
    return policyNumber;
  }
}
