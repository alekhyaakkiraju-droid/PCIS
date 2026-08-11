package com.pcis.masking.scanner;

/** Raised when a database-backed PII scan fails. */
public class PiiScanException extends RuntimeException {

  public PiiScanException(String message, Throwable cause) {
    super(message, cause);
  }
}
