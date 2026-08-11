package com.pcis.premium.contract.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.pcis.premium.contract.ContractSchemaValidator;
import org.junit.jupiter.api.Test;

class PolicyRenewalConsumerContractTest {

  @Test
  void policyRenewalApproveInteractionMatchesContract() throws Exception {
    JsonNode request =
        ContractSchemaValidator.readFixture("/contracts/consumers/policy-renewal-request.json");
    JsonNode response =
        ContractSchemaValidator.readFixture(
            "/contracts/consumers/policy-renewal-response-approve.json");

    ContractSchemaValidator.assertPayloadMatchesSchema("CreateCalculationRequest", request);
    ContractSchemaValidator.assertPayloadMatchesSchema("PremiumCalculationResponse", response);
  }
}
