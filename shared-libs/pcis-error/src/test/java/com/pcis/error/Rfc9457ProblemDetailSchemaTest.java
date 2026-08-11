package com.pcis.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Rfc9457ProblemDetailSchemaTest {

  private static JsonSchema schema;
  private static ObjectMapper objectMapper;

  @BeforeAll
  static void loadSchema() {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    InputStream stream =
        Rfc9457ProblemDetailSchemaTest.class.getResourceAsStream(
            "/schemas/rfc9457-problem-detail.schema.json");
    schema = factory.getSchema(stream);
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Test
  void validProblemDetailPassesSchemaValidation() throws Exception {
    PcisProblemDetail detail =
        ProblemDetailFactory.fromReason(
            ReasonCode.PRM_NOT_IMPLEMENTED,
            org.springframework.http.HttpStatus.NOT_IMPLEMENTED,
            "Premium rating not implemented",
            URI.create("/api/v1/premium/calculations"),
            "corr-123",
            null);

    ObjectNode node = objectMapper.valueToTree(detail);
    Set<ValidationMessage> errors = schema.validate(node);
    assertThat(errors).isEmpty();
  }

  @Test
  void problemDetailWithErrorsArrayIsValid() throws Exception {
    PcisProblemDetail detail =
        ProblemDetailFactory.fromReason(
            ReasonCode.SYS_VALIDATION,
            org.springframework.http.HttpStatus.BAD_REQUEST,
            "Validation failed",
            URI.create("/api/v1/test"),
            "corr-456",
            java.util.List.of(new ProblemErrorEntry("SYS_VALIDATION", "must not be blank", "policyType")));

    ObjectNode node = objectMapper.valueToTree(detail);
    Set<ValidationMessage> errors = schema.validate(node);
    assertThat(errors).isEmpty();
  }

  @Test
  void missingRequiredFieldFailsSchemaValidation() throws Exception {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("title", "Bad");
    node.put("status", 400);
    Set<ValidationMessage> errors = schema.validate(node);
    assertThat(errors).isNotEmpty();
  }
}
