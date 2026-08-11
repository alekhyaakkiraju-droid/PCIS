package com.pcis.masking.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.masking.Classified;
import com.pcis.masking.MaskingService;
import com.pcis.masking.MaskingConstants;
import com.pcis.masking.MaskingTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ClassifiedFieldSerializerModifierTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MaskingService maskingService = MaskingTestSupport.maskingService();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new PcisJacksonMaskingModule(maskingService));
  }

  @Test
  void serializesClassifiedFieldsWithRegistryMasking() throws Exception {
    CustomerResponse dto =
        new CustomerResponse("98-7654321", "john@pcis.test", "John", "PUBLIC");

    String json = objectMapper.writeValueAsString(dto);
    JsonNode node = objectMapper.readTree(json);

    assertThat(node.get("taxId").asText()).isEqualTo("4321");
    assertThat(node.get("email").asText()).isEqualTo("pcis.test");
    assertThat(node.get("firstName").asText()).isEqualTo(MaskingConstants.FULL_REDACT_TOKEN);
    assertThat(node.get("status").asText()).isEqualTo("PUBLIC");
    assertThat(json).doesNotContain("7654321", "john@pcis.test", "John");
  }

  @Test
  void serializesPolymorphicContactValueUsingDiscriminatorField() throws Exception {
    ContactResponse dto = new ContactResponse("EM", "contact@carrier.example");

    String json = objectMapper.writeValueAsString(dto);

    assertThat(objectMapper.readTree(json).get("contactValue").asText()).isEqualTo("carrier.example");
    assertThat(json).doesNotContain("contact@carrier.example");
  }

  @Test
  void fixtureCustomerDtoExpectationsMatch() throws Exception {
    JsonNode fixture =
        new ObjectMapper()
            .readTree(new ClassPathResource("pii-fixtures.json").getInputStream())
            .get("customerDto");

    CustomerResponse dto =
        new CustomerResponse(
            fixture.get("taxId").get("input").asText(),
            fixture.get("email").get("input").asText(),
            fixture.get("firstName").get("input").asText(),
            "ACTIVE");

    JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(dto));

    assertThat(serialized.get("taxId").asText())
        .isEqualTo(fixture.get("taxId").get("expected").asText());
    assertThat(serialized.get("email").asText())
        .isEqualTo(fixture.get("email").get("expected").asText());
    assertThat(serialized.get("firstName").asText())
        .isEqualTo(fixture.get("firstName").get("expected").asText());
  }

  static final class CustomerResponse {
    @Classified(entity = "CUSTOMER_T", column = "TAX_ID")
    public final String taxId;

    @Classified(entity = "CUSTOMER_T", column = "EMAIL")
    public final String email;

    @Classified(entity = "CUSTOMER_T", column = "FIRST_NAME")
    public final String firstName;

    public final String status;

    CustomerResponse(String taxId, String email, String firstName, String status) {
      this.taxId = taxId;
      this.email = email;
      this.firstName = firstName;
      this.status = status;
    }
  }

  static final class ContactResponse {
    @Classified(
        entity = "CUSTOMER_CONTACT_T",
        column = "CONTACT_VALUE",
        discriminatorField = "contactType")
    public final String contactValue;

    public final String contactType;

    ContactResponse(String contactType, String contactValue) {
      this.contactType = contactType;
      this.contactValue = contactValue;
    }
  }
}
