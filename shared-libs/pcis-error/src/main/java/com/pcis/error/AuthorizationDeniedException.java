package com.pcis.error;

public class AuthorizationDeniedException extends PcisException {

  public AuthorizationDeniedException(
      String message, String actor, String resource, String operation) {
    super(ReasonCode.AUTHZ_DENIED_NO_APPROVAL, message, actor, resource, operation, null);
  }
}
