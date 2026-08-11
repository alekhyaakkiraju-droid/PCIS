package com.pcis.customer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for authz-svc authorization decisions.
 * Circuit breaker fallback is fail-closed: denies access when authz-svc is unavailable.
 */
@FeignClient(
    name = "authz-svc",
    url = "${pcis.authz-svc.url}",
    fallback = AuthzServiceClient.FailClosedFallback.class)
public interface AuthzServiceClient {

  @PostMapping("/v1/authz/decisions")
  AuthzDecisionResponse decide(@RequestBody AuthzDecisionRequest request);

  record AuthzDecisionRequest(String subject, String resource, String action) {}

  record AuthzDecisionResponse(boolean permitted, String reason) {}

  /** Fail-closed fallback: circuit open → deny all. */
  @Component
  class FailClosedFallback implements AuthzServiceClient {

    @Override
    public AuthzDecisionResponse decide(AuthzDecisionRequest request) {
      throw new AccessDeniedException(
          "authz-svc unavailable — circuit open; access denied for subject="
              + request.subject() + " resource=" + request.resource());
    }
  }
}
