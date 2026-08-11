package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.ReasonCode;
import org.junit.jupiter.api.Test;

class PaymentAuthorityServiceTest {

  @Test
  void stubReturnsDenyWithPaymentAuthorityReasonCode() {
    var service = new PaymentAuthorityService();
    var request = new AuthorizationRequest("claim", "INITIATE_PAYMENT", null);

    var response =
        service.evaluate(
            "adjuster-001", request, "22222222-2222-2222-2222-222222222222");

    assertThat(response.decision()).isEqualTo(AuthorizationDecision.DENY);
    assertThat(response.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_STUB);
    assertThat(response.evaluatedPermissions()).isEmpty();
  }
}
