package com.pcis.config;

public enum TunableKey {
  AUDIT_RETENTION_DAYS("audit.retention.days", true),
  AUDIT_ARCHIVE_CHUNK_SIZE("audit.archive.chunkSize", true),
  BILLING_LEAD_DAYS("billing.leadDays", true),
  PREMIUM_GRACE_DAYS("premium.graceDays", true),
  POLICY_RENEWAL_WINDOW_DAYS("policy.renewalWindowDays", true),
  CLAIMS_REINSURANCE_CESSION_THRESHOLD("claims.reinsurance.cessionThreshold", true),
  BATCH_RUN_LOG_ENABLED("batch.runLog.enabled", false);

  private final String key;
  private final boolean required;

  TunableKey(String key, boolean required) {
    this.key = key;
    this.required = required;
  }

  public String key() {
    return key;
  }

  public boolean required() {
    return required;
  }
}
