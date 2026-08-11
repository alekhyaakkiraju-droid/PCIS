package com.pcis.authz.application;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.ReasonCode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Stub for claim payment authority rules (approval linkage and cumulative limits). Full
 * implementation arrives in WO-041.
 */
@Service
public class PaymentAuthorityService {

  public AuthorizationResponse evaluate(
      String principalId, AuthorizationRequest request, String correlationId) {
    return new AuthorizationResponse(
        AuthorizationDecision.DENY,
        ReasonCode.PAYMENT_AUTHORITY_STUB,
        List.of(),
        correlationId);
  }
}
