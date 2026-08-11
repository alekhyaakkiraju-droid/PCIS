package com.pcis.premium.contract.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.pcis.premium.contract.ContractSchemaValidator;
import org.junit.jupiter.api.Test;

class BillingConsumerContractTest {

  @Test
  void billingDeclineInteractionMatchesContract() throws Exception {
    JsonNode request =
        ContractSchemaValidator.readFixture("/contracts/consumers/billing-request.json");
    JsonNode response =
        ContractSchemaValidator.readFixture("/contracts/consumers/billing-response-decline.json");

    ContractSchemaValidator.assertPayloadMatchesSchema("CreateCalculationRequest", request);
    ContractSchemaValidator.assertPayloadMatchesSchema("PremiumCalculationResponse", response);
  }
}
