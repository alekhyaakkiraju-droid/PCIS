package com.pcis.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DataClassificationRegistryTest {

  private DataClassificationRegistry registry;

  @BeforeEach
  void setUp() {
    var inMemory =
        InMemoryDataClassificationRegistry.fromDocument(
            ClassificationRegistryParser.parse(
                new ClassPathResource("pcis-data-classification-test.yaml")));
    registry = inMemory.registry();
  }

  @Test
  void getTierReturnsColumnTier() {
    assertThat(registry.getTier("CUSTOMER_T", "TAX_ID")).isEqualTo(DataTier.RESTRICTED);
    assertThat(registry.getTier("customer_t", "tax_id")).isEqualTo(DataTier.RESTRICTED);
    assertThat(registry.getTier("CODE_TABLE_T", "CODE_TYPE")).isEqualTo(DataTier.PUBLIC);
  }

  @Test
  void getMaskStrategyReturnsConfiguredStrategy() {
    assertThat(registry.getMaskStrategy("CUSTOMER_T", "TAX_ID")).isEqualTo(MaskStrategy.LAST_FOUR);
    assertThat(registry.getMaskStrategy("CUSTOMER_T", "EMAIL"))
        .isEqualTo(MaskStrategy.EMAIL_DOMAIN_ONLY);
  }

  @Test
  void getRetentionDaysUsesTierHandling() {
    assertThat(registry.getRetentionDays("CUSTOMER_T", "TAX_ID")).isEqualTo(2555);
    assertThat(registry.getRetentionDays("CODE_TABLE_T", "CODE_TYPE")).isEqualTo(365);
    assertThat(registry.getRetentionDays(DataTier.RESTRICTED)).isEqualTo(2555);
  }

  @Test
  void getAllRestrictedColumnsListsRestrictedTierColumns() {
    var restricted = registry.getAllRestrictedColumns();
    assertThat(restricted).hasSize(3);
    assertThat(restricted)
        .extracting(ClassificationEntry::columnName)
        .containsExactly("CUST_ID", "EMAIL", "TAX_ID");
  }

  @Test
  void missingColumnThrows() {
    assertThatThrownBy(() -> registry.getTier("CUSTOMER_T", "MISSING"))
        .isInstanceOf(ClassificationRegistryException.class)
        .hasMessageContaining("No classification");
  }
}
