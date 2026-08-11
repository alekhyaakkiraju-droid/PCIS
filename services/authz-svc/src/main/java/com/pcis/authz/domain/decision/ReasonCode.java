package com.pcis.authz.domain.decision;

/** Stable reason codes returned with every authorization decision. */
public enum ReasonCode {
  GRANT_MATCH,
  NO_GRANT,
  APPROVAL_MISSING,
  AUTHORITY_LIMIT_EXCEEDED,
  PAYMENT_AUTHORITY_GRANTED,
  /** Approver and disburser are the same principal (segregation-of-duties violation). */
  SELF_APPROVAL_FORBIDDEN,
  /** Batch service accounts may disburse but cannot create approvals. */
  BATCH_CANNOT_APPROVE
}
