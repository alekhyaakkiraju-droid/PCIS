package com.pcis.classification;

/** Raised when the registry and live database schema diverge. */
public class ClassificationDriftException extends ClassificationRegistryException {

  public ClassificationDriftException(String message) {
    super(message);
  }
}
