package com.pcis.batch.reconciliation.domain;

public enum BreakClass {
  MISSING_IN_TARGET,
  MISSING_IN_LEGACY,
  VALUE_MISMATCH,
  COUNT_MISMATCH,
  CHECKSUM_MISMATCH,
  STATUS_MISMATCH
}
