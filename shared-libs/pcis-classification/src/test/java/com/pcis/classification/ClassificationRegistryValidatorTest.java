package com.pcis.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ClassificationRegistryValidatorTest {

  @Test
  void validatesTestFixtureRegistry() {
    var document =
        ClassificationRegistryParser.parse(new ClassPathResource("pcis-data-classification-test.yaml"));
    List<ClassificationEntry> entries = ClassificationRegistryValidator.validateAndFlatten(document);

    assertThat(entries).hasSize(5);
    assertThat(entries.stream().filter(e -> e.tier() == DataTier.RESTRICTED)).hasSize(3);
  }

  @Test
  void rejectsUnknownMaskToken() {
    var document =
        ClassificationRegistryParser.parse(
            new ClassPathResource("pcis-data-classification-invalid-mask.yaml"));

    assertThatThrownBy(() -> ClassificationRegistryValidator.validateAndFlatten(document))
        .isInstanceOf(ClassificationRegistryException.class)
        .hasMessageContaining("Unknown mask strategy");
  }

  @Test
  void rejectsUnknownTier() {
    String yaml =
        """
        registry_version: "1"
        tier_handling: {}
        entities:
          - entity: X
            domain: X
            tier: TopSecret
            columns:
              - column: A
                tier: TopSecret
                pii: false
                mask_strategy: NONE
        """;

    var document = ClassificationRegistryParser.parseYaml(yaml);
    assertThatThrownBy(() -> ClassificationRegistryValidator.validateAndFlatten(document))
        .isInstanceOf(ClassificationRegistryException.class)
        .hasMessageContaining("Unknown data tier");
  }

  @Test
  void rejectsRestrictedPiiWithNoneMask() {
    String yaml =
        """
        registry_version: "1"
        tier_handling:
          Restricted:
            retention_days: 2555
            storage_encryption: at-rest-required
            access_control: need-to-know
            log_emission: masked-redacted
        entities:
          - entity: CUSTOMER_T
            domain: CUS
            tier: Restricted
            columns:
              - column: FIRST_NAME
                tier: Restricted
                pii: true
                mask_strategy: NONE
        """;

    var document = ClassificationRegistryParser.parseYaml(yaml);
    assertThatThrownBy(() -> ClassificationRegistryValidator.validateAndFlatten(document))
        .isInstanceOf(ClassificationRegistryException.class)
        .hasMessageContaining("must declare a non-NONE mask_strategy");
  }

  @Test
  void rejectsDuplicateEntityColumnKeys() {
    String yaml =
        """
        registry_version: "1"
        tier_handling:
          Internal:
            retention_days: 2555
            storage_encryption: at-rest-optional
            access_control: authenticated
            log_emission: allowed
        entities:
          - entity: CUSTOMER_T
            domain: CUS
            tier: Internal
            columns:
              - column: CUST_ID
                tier: Internal
                pii: false
                mask_strategy: NONE
              - column: CUST_ID
                tier: Internal
                pii: false
                mask_strategy: NONE
        """;

    var document = ClassificationRegistryParser.parseYaml(yaml);
    assertThatThrownBy(() -> ClassificationRegistryValidator.validateAndFlatten(document))
        .isInstanceOf(ClassificationRegistryException.class)
        .hasMessageContaining("Duplicate classification key");
  }
}
