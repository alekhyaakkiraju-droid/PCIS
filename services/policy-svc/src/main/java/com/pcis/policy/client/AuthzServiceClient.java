package com.pcis.policy.client;

import com.pcis.policy.exception.AuthzServiceUnavailableException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "authz-svc",
    url = "${pcis.authz-svc.url}",
    fallback = AuthzServiceClient.FailClosedFallback.class)
public interface AuthzServiceClient {

  @PostMapping("/v1/authz/decisions")
  AuthzDecisionResponse decide(@RequestBody AuthzDecisionRequest request);

  record AuthzDecisionRequest(String subject, String resource, String action) {}

  record AuthzDecisionResponse(boolean permitted, String reason) {}

  @Component
  class FailClosedFallback implements AuthzServiceClient {

    @Override
    public AuthzDecisionResponse decide(AuthzDecisionRequest request) {
      throw new AuthzServiceUnavailableException(
          "authz-svc unavailable — circuit open; access denied for subject="
              + request.subject()
              + " resource="
              + request.resource());
    }
  }
}
