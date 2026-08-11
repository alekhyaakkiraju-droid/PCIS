package com.pcis.error;

public class ValidationException extends PcisException {

  public ValidationException(String message, String actor, String resource, String operation) {
    super(ReasonCode.SYS_VALIDATION, message, actor, resource, operation, null);
  }
}
