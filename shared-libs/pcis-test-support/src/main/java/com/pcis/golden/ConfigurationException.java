package com.pcis.golden;

/**
 * Thrown when golden normalization configuration violates deny-list rules
 * or other capture invariants.
 */
public class ConfigurationException extends RuntimeException {

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
