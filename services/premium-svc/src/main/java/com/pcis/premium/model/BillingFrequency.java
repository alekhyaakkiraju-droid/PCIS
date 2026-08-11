package com.pcis.premium.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Legacy BIL003B billing frequency codes (WO-188). */
public enum BillingFrequency {
  M(12),
  Q(4),
  S(2),
  A(1);

  private static final Logger log = LoggerFactory.getLogger(BillingFrequency.class);

  private final int installmentCount;

  BillingFrequency(int installmentCount) {
    this.installmentCount = installmentCount;
  }

  public int getInstallmentCount() {
    return installmentCount;
  }

  public static BillingFrequency fromCode(String code) {
    if (code == null || code.isBlank()) {
      log.warn("Unknown billing frequency code '{}'; defaulting to annual (A)", code);
      return A;
    }
    String normalized = code.trim().toUpperCase();
    for (BillingFrequency frequency : values()) {
      if (frequency.name().equals(normalized)) {
        return frequency;
      }
    }
    log.warn("Unknown billing frequency code '{}'; defaulting to annual (A)", code);
    return A;
  }
}
