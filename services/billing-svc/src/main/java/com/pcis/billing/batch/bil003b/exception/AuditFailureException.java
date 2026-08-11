package com.pcis.billing.batch.bil003b.exception;

/** Outbox/audit write failure — triggers transaction rollback and exit code 4. */
public class AuditFailureException extends RuntimeException {

  public AuditFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
