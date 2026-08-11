package com.pcis.error;

public class ResourceNotFoundException extends PcisException {

  public ResourceNotFoundException(
      String message, String actor, String resource, String operation) {
    super(ReasonCode.SYS_NOT_FOUND, message, actor, resource, operation, null);
  }
}
