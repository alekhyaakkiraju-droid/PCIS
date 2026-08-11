package com.pcis.classification;

/** Raised when the classification registry YAML or runtime state is invalid. */
public class ClassificationRegistryException extends RuntimeException {

  public ClassificationRegistryException(String message) {
    super(message);
  }

  public ClassificationRegistryException(String message, Throwable cause) {
    super(message, cause);
  }
}
