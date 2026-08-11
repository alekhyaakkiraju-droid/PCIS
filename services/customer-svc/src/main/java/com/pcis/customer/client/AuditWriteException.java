package com.pcis.customer.client;

/** Thrown when audit-svc is unavailable to prevent mutation without an audit record. */
public class AuditWriteException extends RuntimeException {

  public AuditWriteException(String message) {
    super(message);
  }
}
