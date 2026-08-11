package com.pcis.error;

public abstract class PcisException extends RuntimeException {

  private final ReasonCode reasonCode;
  private final String actor;
  private final String resource;
  private final String operation;

  protected PcisException(
      ReasonCode reasonCode,
      String message,
      String actor,
      String resource,
      String operation,
      Throwable cause) {
    super(message, cause);
    this.reasonCode = reasonCode;
    this.actor = actor;
    this.resource = resource;
    this.operation = operation;
  }

  public ReasonCode reasonCode() {
    return reasonCode;
  }

  public String actor() {
    return actor;
  }

  public String resource() {
    return resource;
  }

  public String operation() {
    return operation;
  }
}
