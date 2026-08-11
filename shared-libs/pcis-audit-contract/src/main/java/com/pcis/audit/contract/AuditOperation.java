package com.pcis.audit.contract;

/** Canonical operation derived from legacy action codes (audlog01-v1 mapping_table). */
public enum AuditOperation {
  CREATE,
  UPDATE,
  DELETE,
  PAY,
  RENEW,
  BILL,
  INIT,
  FINALIZE
}
