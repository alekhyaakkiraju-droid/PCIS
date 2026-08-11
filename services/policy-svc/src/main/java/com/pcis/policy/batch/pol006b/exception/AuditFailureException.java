package com.pcis.policy.batch.pol006b.exception;

public class AuditFailureException extends RuntimeException {

  public AuditFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
