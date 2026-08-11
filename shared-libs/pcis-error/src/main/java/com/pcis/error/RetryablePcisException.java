package com.pcis.error;

public class RetryablePcisException extends PcisException {

  public RetryablePcisException(
      ReasonCode reasonCode,
      String message,
      String actor,
      String resource,
      String operation) {
    super(reasonCode, message, actor, resource, operation, null);
  }

  public RetryablePcisException(
      ReasonCode reasonCode,
      String message,
      String actor,
      String resource,
      String operation,
      Throwable cause) {
    super(reasonCode, message, actor, resource, operation, cause);
  }
}
