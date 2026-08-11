package com.pcis.batch.audit.config;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import org.springframework.stereotype.Service;

/** Resolves audit retention days from pcis-config tunables with property fallback. */
@Service
public class RetentionConfigService {

  public static final int POLICY_MINIMUM_RETENTION_DAYS = 365;

  private final TunableResolver tunableResolver;
  private final AuditArchiveProperties properties;

  public RetentionConfigService(
      TunableResolver tunableResolver, AuditArchiveProperties properties) {
    this.tunableResolver = tunableResolver;
    this.properties = properties;
  }

  /** Validates every configured data-classification tier meets the 365-day floor. */
  public void validateTierRetentionFloors() {
    for (int days : properties.getRetention().allTierDays()) {
      enforceMinimum(days, "tier retention");
    }
  }

  public int getRetentionDays() {
    int days;
    try {
      days = tunableResolver.getInt(TunableKey.AUDIT_RETENTION_DAYS);
    } catch (TunableNotFoundException ex) {
      days = properties.getRetentionDays();
    }
    enforceMinimum(days, "audit.retention.days");
    validateTierRetentionFloors();
    return days;
  }

  public int getRetentionDaysForTier(String tier) {
    validateTierRetentionFloors();
    return switch (tier.toUpperCase()) {
      case "PUBLIC" -> properties.getRetention().getPublicDays();
      case "INTERNAL" -> properties.getRetention().getInternalDays();
      case "CONFIDENTIAL" -> properties.getRetention().getConfidentialDays();
      case "RESTRICTED" -> properties.getRetention().getRestrictedDays();
      default -> throw new IllegalStateException("Unknown data classification tier: " + tier);
    };
  }

  private static void enforceMinimum(int days, String label) {
    if (days < POLICY_MINIMUM_RETENTION_DAYS) {
      throw new IllegalStateException(
          label + "=" + days + " is below the " + POLICY_MINIMUM_RETENTION_DAYS + "-day policy minimum");
    }
  }
}
