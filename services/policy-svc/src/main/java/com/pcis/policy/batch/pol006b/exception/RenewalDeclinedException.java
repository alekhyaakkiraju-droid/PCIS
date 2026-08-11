package com.pcis.policy.batch.pol006b.exception;

public class RenewalDeclinedException extends RenewalException {

  public RenewalDeclinedException(String policyNumber, String reason) {
    super(policyNumber, "DECLINE", reason);
  }
}
