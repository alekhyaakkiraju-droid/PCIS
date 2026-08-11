package com.pcis.golden;

/** Categories of divergence between golden expected and actual batch output. */
public enum DiffCategory {
  ONE_CENT_DIVERGENCE,
  MISSING_ROW,
  EXTRA_ROW,
  STATUS_MISMATCH,
  COUNTER_MISMATCH,
  TYPE_MISMATCH,
  VALUE_MISMATCH
}
