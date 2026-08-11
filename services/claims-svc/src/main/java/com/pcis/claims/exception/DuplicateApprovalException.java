package com.pcis.claims.exception;

/** Raised when an active approval already exists for a reserve. */
public class DuplicateApprovalException extends RuntimeException {

  public DuplicateApprovalException(Long reserveId) {
    super("Active approval already exists for reserve: " + reserveId);
  }
}
