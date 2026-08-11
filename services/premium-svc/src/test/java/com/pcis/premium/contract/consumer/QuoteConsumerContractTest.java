package com.pcis.premium.contract.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.pcis.premium.contract.ContractSchemaValidator;
import org.junit.jupiter.api.Test;

class QuoteConsumerContractTest {

  @Test
  void quoteReferInteractionMatchesContract() throws Exception {
    JsonNode request =
        ContractSchemaValidator.readFixture("/contracts/consumers/quote-request.json");
    JsonNode response =
        ContractSchemaValidator.readFixture("/contracts/consumers/quote-response-refer.json");

    ContractSchemaValidator.assertPayloadMatchesSchema("CreateCalculationRequest", request);
    ContractSchemaValidator.assertPayloadMatchesSchema("PremiumCalculationResponse", response);
  }

  @Test
  void callerInputErrorProblemMatchesContract() throws Exception {
    JsonNode problem =
        ContractSchemaValidator.readFixture(
            "/contracts/consumers/caller-input-error-problem.json");
    ContractSchemaValidator.assertPayloadMatchesSchema("ProblemDetail", problem);
  }
}
