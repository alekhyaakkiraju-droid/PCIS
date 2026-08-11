package com.pcis.batch.audit.config;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import org.springframework.stereotype.Service;

/** Resolves audit retention days from pcis-config tunables with property fallback. */
@Service
public class RetentionConfigService {

  static final int POLICY_MINIMUM_RETENTION_DAYS = 365;

  private final TunableResolver tunableResolver;
  private final AuditArchiveProperties properties;

  public RetentionConfigService(
      TunableResolver tunableResolver, AuditArchiveProperties properties) {
    this.tunableResolver = tunableResolver;
    this.properties = properties;
  }

  public int getRetentionDays() {
    int days;
    try {
      days = tunableResolver.getInt(TunableKey.AUDIT_RETENTION_DAYS);
    } catch (TunableNotFoundException ex) {
      days = properties.getRetentionDays();
    }
    if (days < POLICY_MINIMUM_RETENTION_DAYS) {
      throw new IllegalStateException(
          "audit.retention.days="
              + days
              + " is below the "
              + POLICY_MINIMUM_RETENTION_DAYS
              + "-day policy minimum");
    }
    return days;
  }
}
