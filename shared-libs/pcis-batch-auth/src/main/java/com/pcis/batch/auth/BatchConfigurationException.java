package com.pcis.batch.auth;

/**
 * Raised when batch OAuth2 configuration or token acquisition fails.
 *
 * <p>Maps to Spring Batch / Kubernetes exit code {@value #EXIT_CODE} per the WO-137 contract
 * (configuration-validation failure).
 */
public class BatchConfigurationException extends RuntimeException {

  public static final int EXIT_CODE = 5;

  public BatchConfigurationException(String message) {
    super(message);
  }

  public BatchConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public int getExitCode() {
    return EXIT_CODE;
  }
}
