package com.pcis.error;

public class ConflictException extends PcisException {

  public ConflictException(String message, String actor, String resource, String operation) {
    super(ReasonCode.SYS_CONFLICT, message, actor, resource, operation, null);
  }
}
