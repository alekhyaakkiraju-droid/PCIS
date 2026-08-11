package com.pcis.authz.domain.decision;

/** Stable reason codes returned with every authorization decision. */
public enum ReasonCode {
  GRANT_MATCH,
  NO_GRANT,
  APPROVAL_MISSING,
  AUTHORITY_LIMIT_EXCEEDED,
  PAYMENT_AUTHORITY_GRANTED
}
