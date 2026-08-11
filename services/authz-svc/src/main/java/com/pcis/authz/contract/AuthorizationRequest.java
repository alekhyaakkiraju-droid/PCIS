package com.pcis.authz.contract;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** Inbound policy decision request. Actor is resolved from the authenticated JWT subject. */
public record AuthorizationRequest(
    @NotBlank String resource,
    @NotBlank String operation,
    Map<String, Object> context) {

  public AuthorizationRequest {
    context = context == null ? Map.of() : Map.copyOf(context);
  }
}
