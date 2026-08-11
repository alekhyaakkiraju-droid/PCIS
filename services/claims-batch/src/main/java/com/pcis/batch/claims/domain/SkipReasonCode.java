package com.pcis.batch.claims.domain;

public enum SkipReasonCode {
  NO_APPROVAL,
  SAME_PRINCIPAL,
  EXCEEDS_AUTHORITY,
  ZERO_OUTSTANDING,
  INSUFFICIENT_RESERVE
}
