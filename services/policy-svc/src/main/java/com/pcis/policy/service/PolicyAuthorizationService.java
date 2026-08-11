package com.pcis.policy.service;

import com.pcis.policy.client.AuthzServiceClient;
import com.pcis.policy.client.AuthzServiceClient.AuthzDecisionRequest;
import com.pcis.policy.exception.AuthzServiceUnavailableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PolicyAuthorizationService {

  private final AuthzServiceClient authzServiceClient;

  public PolicyAuthorizationService(AuthzServiceClient authzServiceClient) {
    this.authzServiceClient = authzServiceClient;
  }

  public void requireMutationPermitted(String subject, String policyNumber) {
    try {
      AuthzServiceClient.AuthzDecisionResponse response =
          authzServiceClient.decide(
              new AuthzDecisionRequest(subject, "policy:" + policyNumber, "modify"));
      if (!response.permitted()) {
        throw new AccessDeniedException(
            "authz-svc denied modify for policy="
                + policyNumber
                + (response.reason() != null ? ": " + response.reason() : ""));
      }
    } catch (AuthzServiceUnavailableException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new AuthzServiceUnavailableException(
          "authz-svc call failed for policy=" + policyNumber + ": " + ex.getMessage());
    }
  }
}
