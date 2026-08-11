package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.ReasonCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyDecisionServiceTest {

  private static final String CORRELATION_ID = "11111111-1111-1111-1111-111111111111";

  @Mock private PermissionResolver permissionResolver;
  @Mock private PaymentAuthorityService paymentAuthorityService;

  private PolicyDecisionService policyDecisionService;

  @BeforeEach
  void setUp() {
    policyDecisionService = new PolicyDecisionService(permissionResolver, paymentAuthorityService);
  }

  @Test
  void permitsWhenPrincipalHasMatchingGrant() {
    when(permissionResolver.resolvePermissionCodes("adjuster-001"))
        .thenReturn(List.of("claim:read", "claim:pay"));

    var response =
        policyDecisionService.evaluate(
            "adjuster-001", new AuthorizationRequest("claim", "read", null), CORRELATION_ID);

    assertThat(response.decision()).isEqualTo(AuthorizationDecision.PERMIT);
    assertThat(response.reasonCode()).isEqualTo(ReasonCode.GRANT_MATCH);
    assertThat(response.evaluatedPermissions()).containsExactly("claim:read", "claim:pay");
  }

  @Test
  void deniesByDefaultWhenGrantMissing() {
    when(permissionResolver.resolvePermissionCodes("adjuster-001"))
        .thenReturn(List.of("claim:read", "claim:pay"));

    var response =
        policyDecisionService.evaluate(
            "adjuster-001",
            new AuthorizationRequest("customer", "write", null),
            CORRELATION_ID);

    assertThat(response.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(response.reasonCode()).isEqualTo(ReasonCode.NO_GRANT);
  }

  @Test
  void deniesWhenPrincipalHasNoRoleAssignments() {
    when(permissionResolver.resolvePermissionCodes("unknown-user")).thenReturn(List.of());

    for (AuthorizationRequest request : PolicyDecisionService.allSeededResourceOperations()) {
      var response =
          policyDecisionService.evaluate("unknown-user", request, CORRELATION_ID);
      assertThat(response.decision()).isEqualTo(AuthorizationDecision.DENY);
      assertThat(response.reasonCode()).isEqualTo(ReasonCode.NO_GRANT);
    }
  }

  @Test
  void routesInitiatePaymentToPaymentAuthorityService() {
    var request = new AuthorizationRequest("claim", "INITIATE_PAYMENT", null);
    var stubResponse =
        new com.pcis.authz.contract.AuthorizationResponse(
            AuthorizationDecision.DENY,
            ReasonCode.PAYMENT_AUTHORITY_STUB,
            List.of(),
            CORRELATION_ID);
    when(paymentAuthorityService.evaluate("adjuster-001", request, CORRELATION_ID))
        .thenReturn(stubResponse);

    var response = policyDecisionService.evaluate("adjuster-001", request, CORRELATION_ID);

    assertThat(response.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_STUB);
  }

  @Test
  void routesApprovePaymentToPaymentAuthorityService() {
    var request = new AuthorizationRequest("claim", "approve_payment", null);
    var stubResponse =
        new com.pcis.authz.contract.AuthorizationResponse(
            AuthorizationDecision.DENY,
            ReasonCode.PAYMENT_AUTHORITY_STUB,
            List.of(),
            CORRELATION_ID);
    when(paymentAuthorityService.evaluate("adjuster-001", request, CORRELATION_ID))
        .thenReturn(stubResponse);

    var response = policyDecisionService.evaluate("adjuster-001", request, CORRELATION_ID);

    assertThat(response.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_STUB);
  }

  @Test
  void requiredPermissionCodeUsesResourceAndOperation() {
    assertThat(PolicyDecisionService.requiredPermissionCode("claim", "read"))
        .isEqualTo("claim:read");
  }
}
