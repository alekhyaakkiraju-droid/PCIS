package com.pcis.golden;

/** Thrown when normalization configuration violates monetary/status deny-list rules. */
public final class ConfigurationValidationException extends RuntimeException {

  public ConfigurationValidationException(String message) {
    super(message);
  }

  public ConfigurationValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
