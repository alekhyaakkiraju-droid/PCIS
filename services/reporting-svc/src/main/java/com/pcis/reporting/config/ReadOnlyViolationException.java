package com.pcis.reporting.config;

public class ReadOnlyViolationException extends RuntimeException {
  public ReadOnlyViolationException(String message) {
    super(message);
  }
}
