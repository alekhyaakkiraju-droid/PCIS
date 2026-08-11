package com.pcis.authz.application;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.PaymentOperations;
import com.pcis.authz.domain.decision.ReasonCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Deny-by-default permission evaluator. Payment operations are delegated to
 * {@link PaymentAuthorityService}.
 */
@Service
public class PolicyDecisionService {

  private final PermissionResolver permissionResolver;
  private final PaymentAuthorityService paymentAuthorityService;

  public PolicyDecisionService(
      PermissionResolver permissionResolver, PaymentAuthorityService paymentAuthorityService) {
    this.permissionResolver = permissionResolver;
    this.paymentAuthorityService = paymentAuthorityService;
  }

  public AuthorizationResponse evaluate(
      String principalId, AuthorizationRequest request, String correlationId) {
    if (PaymentOperations.isPaymentOperation(request.operation())) {
      return paymentAuthorityService.evaluate(principalId, request, correlationId);
    }

    List<String> evaluatedPermissions = permissionResolver.resolvePermissionCodes(principalId);
    String requiredPermission = requiredPermissionCode(request.resource(), request.operation());

    if (evaluatedPermissions.contains(requiredPermission)) {
      return new AuthorizationResponse(
          AuthorizationDecision.PERMIT,
          ReasonCode.GRANT_MATCH,
          List.copyOf(evaluatedPermissions),
          correlationId);
    }

    return new AuthorizationResponse(
        AuthorizationDecision.DENY,
        ReasonCode.NO_GRANT,
        List.copyOf(evaluatedPermissions),
        correlationId);
  }

  static String requiredPermissionCode(String resource, String operation) {
    return resource.trim() + ":" + operation.trim();
  }

  /** Returns all seeded resource/operation pairs for deny-by-default integration tests. */
  public static List<AuthorizationRequest> allSeededResourceOperations() {
    List<AuthorizationRequest> requests = new ArrayList<>();
    requests.add(new AuthorizationRequest("claim", "read", Map.of()));
    requests.add(new AuthorizationRequest("claim", "pay", Map.of()));
    requests.add(new AuthorizationRequest("customer", "read", Map.of()));
    requests.add(new AuthorizationRequest("customer", "write", Map.of()));
    requests.add(new AuthorizationRequest("batch", "execute", Map.of()));
    return List.copyOf(requests);
  }
}
