package com.pcis.classification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassificationCompletenessCheckerTest {

  private DataClassificationRegistry registry;

  @BeforeEach
  void setUp() {
    registry = InMemoryDataClassificationRegistry.fromYaml(
            """
            registry_version: "1.0.0"
            entities:
              - entity: CUSTOMER_T
                tier: RESTRICTED
                columns:
                  - column: TAX_ID
                    tier: RESTRICTED
                    mask_strategy: LAST_FOUR
                    pii: true
              - entity: POLICY_T
                tier: INTERNAL
                columns:
                  - column: POLICY_NBR
                    tier: INTERNAL
                    mask_strategy: NONE
                    pii: false
            """)
        .registry();
  }

  @Test
  void passesWhenAllSchemaTablesAreClassified() {
    CompletenessReport report =
        ClassificationCompletenessChecker.checkCompleteness(
            Set.of("CUSTOMER_T", "POLICY_T"), registry);
    assertTrue(report.passed());
  }

  @Test
  void failsWhenSchemaTableIsUnclassified() {
    CompletenessReport report =
        ClassificationCompletenessChecker.checkCompleteness(
            Set.of("CUSTOMER_T", "POLICY_T", "MYSTERY_T"), registry);
    assertFalse(report.passed());
    assertTrue(report.unclassifiedTables().contains("MYSTERY_T"));
  }

  @Test
  void failsWhenRestrictedPiiColumnHasNoMaskStrategy() {
    var badRegistry = new DataClassificationRegistry();
    badRegistry.replaceAll(
        List.of(
            new ClassificationEntry(
                "CUSTOMER_T",
                "TAX_ID",
                DataTier.RESTRICTED,
                MaskStrategy.NONE,
                2555,
                true,
                null,
                "test")));
    CompletenessReport report =
        ClassificationCompletenessChecker.checkCompleteness(Set.of("CUSTOMER_T"), badRegistry);
    assertFalse(report.passed());
    assertFalse(report.restrictedColumnsWithoutStrategy().isEmpty());
  }
}
