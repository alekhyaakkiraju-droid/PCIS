package com.pcis.config.rules;

public enum RuleSetKey {
  BILLING_FREQUENCY_INTERVAL("billing-frequency-interval"),
  DELINQUENCY_STATUS_TRANSITION("delinquency-status-transition");

  private final String key;

  RuleSetKey(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }
}
