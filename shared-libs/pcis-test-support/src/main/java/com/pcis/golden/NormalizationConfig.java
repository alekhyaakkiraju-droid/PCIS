package com.pcis.golden;

import java.nio.file.Path;

/**
 * Loads {@code golden/normalization-rules.yaml} and validates that no monetary or status
 * columns appear on the normalization allow-list before comparison begins.
 */
public final class NormalizationConfig {

  private final NormalizationRules rules;
  private final NormalizationConfigValidator validator;

  private NormalizationConfig(NormalizationRules rules) {
    this.rules = rules;
    this.validator = new NormalizationConfigValidator(rules);
  }

  public static NormalizationConfig load(Path yamlPath) {
    NormalizationRules rules = NormalizationRules.load(yamlPath);
    NormalizationConfig config = new NormalizationConfig(rules);
    config.validateBeforeComparison();
    return config;
  }

  public static NormalizationConfig loadFromClasspath(String resource) {
    NormalizationRules rules = NormalizationRules.loadFromClasspath(resource);
    NormalizationConfig config = new NormalizationConfig(rules);
    config.validateBeforeComparison();
    return config;
  }

  public static NormalizationConfig fromRules(NormalizationRules rules) {
    NormalizationConfig config = new NormalizationConfig(rules);
    config.validateBeforeComparison();
    return config;
  }

  /** Fail-fast validation before any comparison begins. */
  public void validateBeforeComparison() {
    try {
      validator.validateRulesConsistency();
    } catch (ConfigurationException e) {
      throw new ConfigurationValidationException(e.getMessage(), e);
    }
  }

  /**
   * Validates a proposed allow-list extension; rejects monetary NUMERIC and status columns.
   */
  public void validateProposedAllowList(java.util.Collection<String> columns) {
    try {
      validator.validateAllowList(columns);
    } catch (ConfigurationException e) {
      throw new ConfigurationValidationException(e.getMessage(), e);
    }
  }

  public NormalizationRules rules() {
    return rules;
  }
}
