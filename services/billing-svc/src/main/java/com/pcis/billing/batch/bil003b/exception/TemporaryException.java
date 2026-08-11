package com.pcis.billing.batch.bil003b.exception;

/** Transient failure eligible for Spring Batch retry. */
public class TemporaryException extends RuntimeException {

  public TemporaryException(String message) {
    super(message);
  }

  public TemporaryException(String message, Throwable cause) {
    super(message, cause);
  }
}
