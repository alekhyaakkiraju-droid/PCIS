package com.pcis.audit.contract;

/** Raised when a legacy action code cannot be mapped to {@link AuditActionCode}. */
public class UnknownAuditActionException extends RuntimeException {

  private final String legacyAction;

  public UnknownAuditActionException(String legacyAction) {
    super("Unknown audit action code: " + legacyAction);
    this.legacyAction = legacyAction;
  }

  public String legacyAction() {
    return legacyAction;
  }
}
