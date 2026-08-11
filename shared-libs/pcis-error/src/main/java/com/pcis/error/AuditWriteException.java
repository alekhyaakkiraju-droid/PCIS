package com.pcis.error;

public class AuditWriteException extends PcisException {

  public AuditWriteException(String message, String actor, String resource, String operation) {
    super(ReasonCode.AUD_WRITE_FAILURE, message, actor, resource, operation, null);
  }

  public AuditWriteException(
      String message, String actor, String resource, String operation, Throwable cause) {
    super(ReasonCode.AUD_WRITE_FAILURE, message, actor, resource, operation, cause);
  }
}
