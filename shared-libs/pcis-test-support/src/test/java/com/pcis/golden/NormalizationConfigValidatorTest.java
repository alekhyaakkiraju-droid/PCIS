package com.pcis.golden;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormalizationConfigValidatorTest {

  private NormalizationConfigValidator validator;

  @BeforeEach
  void setUp() {
    NormalizationRules rules =
        NormalizationRules.loadFromClasspath("normalization-rules.yaml");
    validator = new NormalizationConfigValidator(rules);
  }

  @Test
  void acceptsTimestampAndSurrogateAllowList() {
    assertDoesNotThrow(
        () ->
            validator.validateAllowList(
                List.of("CREATED_AT", "UPDATED_AT", "PAYMENT_ID", "RUN_ID", "LOG_TIMESTAMP")));
  }

  @Test
  void rejectsMonetaryNumericColumnsOnAllowList() {
    ConfigurationException ex =
        assertThrows(
            ConfigurationException.class,
            () -> validator.validateAllowList(List.of("CREATED_AT", "PAYMENT_AMT", "AMOUNT")));
    assertTrue(ex.getMessage().contains("PAYMENT_AMT"));
    assertTrue(ex.getMessage().contains("AMOUNT"));
  }

  @Test
  void rejectsStatusColumnsOnAllowList() {
    ConfigurationException ex =
        assertThrows(
            ConfigurationException.class,
            () ->
                validator.validateAllowList(
                    List.of("RESERVE_STATUS", "STATUS", "COMM_CALC_FLAG")));
    assertTrue(ex.getMessage().contains("STATUS"));
  }

  @Test
  void rulesFileItselfIsConsistent() {
    assertDoesNotThrow(validator::validateRulesConsistency);
  }
}
