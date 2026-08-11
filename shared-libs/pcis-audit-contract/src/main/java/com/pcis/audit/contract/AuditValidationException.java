package com.pcis.audit.contract;

import java.util.List;

/** Validation failure for audit event requests. */
public class AuditValidationException extends RuntimeException {

  private final List<String> violations;

  public AuditValidationException(List<String> violations) {
    super(String.join("; ", violations));
    this.violations = List.copyOf(violations);
  }

  public List<String> violations() {
    return violations;
  }
}
