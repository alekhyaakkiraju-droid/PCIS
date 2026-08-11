package com.pcis.policy.exception;

public class AuthzServiceUnavailableException extends RuntimeException {

  public AuthzServiceUnavailableException(String message) {
    super(message);
  }
}
