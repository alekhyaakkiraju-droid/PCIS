package com.pcis.billing.domain.exception;

public class NoOutstandingBalanceException extends RuntimeException {

  public NoOutstandingBalanceException(String polNbr) {
    super("No outstanding balance for policy: " + polNbr);
  }
}
