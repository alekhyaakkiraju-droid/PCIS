package com.pcis.premium.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.InputStream;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

public final class ContractSchemaValidator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ContractSchemaValidator() {}

  public static void assertPayloadMatchesSchema(String schemaName, JsonNode payload) throws Exception {
    var openApi =
        new OpenAPIV3Parser()
            .readContents(PremiumRatingContractSupport.contractYaml(), null, null)
            .getOpenAPI();
    Schema<?> schema = openApi.getComponents().getSchemas().get(schemaName);
    assertThat(schema).as("schema %s must exist in contract", schemaName).isNotNull();
    validateNode(payload, schema, schemaName);
  }

  public static JsonNode readFixture(String classpathResource) throws Exception {
    try (InputStream in =
        ContractSchemaValidator.class.getResourceAsStream(classpathResource)) {
      assertThat(in).as("fixture %s must exist", classpathResource).isNotNull();
      return MAPPER.readTree(in);
    }
  }

  @SuppressWarnings("rawtypes")
  private static void validateNode(JsonNode node, Schema schema, String path) {
    if (schema.getRequired() != null) {
      for (Object requiredField : schema.getRequired()) {
        String fieldName = requiredField.toString();
        assertThat(node.has(fieldName))
            .as("%s must contain required field %s", path, fieldName)
            .isTrue();
      }
    }

    if (schema.getProperties() != null) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        Schema propertySchema = (Schema) schema.getProperties().get(entry.getKey());
        assertThat(propertySchema)
            .as("%s must not contain unknown field %s", path, entry.getKey())
            .isNotNull();
        validateTypedValue(entry.getValue(), propertySchema, path + "." + entry.getKey());
      }
    }

    if (schema.getEnum() != null && node.isTextual()) {
      List<String> allowed = schema.getEnum().stream().map(Object::toString).toList();
      assertThat(allowed).contains(node.asText());
    }
  }

  @SuppressWarnings("rawtypes")
  private static void validateTypedValue(JsonNode value, Schema schema, String path) {
    if (schema.get$ref() != null) {
      String refName = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
      var openApi =
          new OpenAPIV3Parser()
              .readContents(PremiumRatingContractSupport.contractYaml(), null, null)
              .getOpenAPI();
      Schema<?> refSchema = openApi.getComponents().getSchemas().get(refName);
      validateNode(value, refSchema, path);
      return;
    }

    if ("array".equals(schema.getType())) {
      assertThat(value.isArray()).as("%s must be an array", path).isTrue();
      for (JsonNode element : value) {
        validateNode(element, schema.getItems(), path + "[]");
      }
      return;
    }

    if (schema.getEnum() != null && value.isTextual()) {
      List<String> allowed = schema.getEnum().stream().map(Object::toString).toList();
      assertThat(allowed).as("%s enum", path).contains(value.asText());
    }
  }
}
