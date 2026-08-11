package com.pcis.classification;

/** GDPR/ISO 27001 data classification tiers for PCIS entities and columns. */
public enum DataTier {
  PUBLIC("Public"),
  INTERNAL("Internal"),
  CONFIDENTIAL("Confidential"),
  RESTRICTED("Restricted");

  private final String yamlValue;

  DataTier(String yamlValue) {
    this.yamlValue = yamlValue;
  }

  public String yamlValue() {
    return yamlValue;
  }

  public static DataTier fromYaml(String value) {
    if (value == null || value.isBlank()) {
      throw new ClassificationRegistryException("Data tier must not be blank");
    }
    for (DataTier tier : values()) {
      if (tier.yamlValue.equalsIgnoreCase(value.trim())) {
        return tier;
      }
    }
    throw new ClassificationRegistryException("Unknown data tier: " + value);
  }
}
