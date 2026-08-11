package com.pcis.premium.contract.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.pcis.premium.contract.ContractSchemaValidator;
import org.junit.jupiter.api.Test;

class PolicyIssuanceConsumerContractTest {

  @Test
  void policyIssuanceApproveInteractionMatchesContract() throws Exception {
    JsonNode request =
        ContractSchemaValidator.readFixture(
            "/contracts/consumers/policy-issuance-request.json");
    JsonNode response =
        ContractSchemaValidator.readFixture(
            "/contracts/consumers/policy-issuance-response-approve.json");

    ContractSchemaValidator.assertPayloadMatchesSchema("CreateCalculationRequest", request);
    ContractSchemaValidator.assertPayloadMatchesSchema("PremiumCalculationResponse", response);
  }
}
