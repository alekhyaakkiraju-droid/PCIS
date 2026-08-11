package com.pcis.authz.domain.decision;

/** Stable reason codes returned with every authorization decision. */
public enum ReasonCode {
  GRANT_MATCH,
  NO_GRANT,
  PAYMENT_AUTHORITY_STUB
}
