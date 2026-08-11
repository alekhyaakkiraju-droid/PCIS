package com.pcis.classification;

/** PII masking strategy tokens applied at log, audit, and API emission time. */
public enum MaskStrategy {
  NONE,
  LAST_FOUR,
  EMAIL_DOMAIN_ONLY,
  PHONE_LAST_FOUR,
  DATE_YEAR_ONLY,
  FULL_REDACT;

  public static MaskStrategy fromYaml(String value) {
    if (value == null || value.isBlank()) {
      throw new ClassificationRegistryException("Mask strategy must not be blank");
    }
    try {
      return MaskStrategy.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ClassificationRegistryException("Unknown mask strategy: " + value);
    }
  }
}
