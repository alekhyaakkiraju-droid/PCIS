package com.pcis.error;

public class TerminalPcisException extends PcisException {

  public TerminalPcisException(
      ReasonCode reasonCode,
      String message,
      String actor,
      String resource,
      String operation) {
    super(reasonCode, message, actor, resource, operation, null);
  }
}
