package com.pcis.billing.domain.exception;

public class PolicyNotFoundException extends RuntimeException {

  public PolicyNotFoundException(String polNbr) {
    super("Policy not found: " + polNbr);
  }
}
