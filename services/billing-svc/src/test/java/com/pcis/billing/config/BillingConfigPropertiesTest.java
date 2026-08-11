package com.pcis.billing.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BillingConfigPropertiesTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void defaultsAreValid() {
    BillingConfigProperties properties = new BillingConfigProperties();

    Set<ConstraintViolation<BillingConfigProperties>> violations = validator.validate(properties);
    assertThat(violations).isEmpty();
    assertThat(properties.getLeadDays()).isEqualTo(15);
    assertThat(properties.getGraceDays()).isEqualTo(10);
    assertThat(properties.getFrequencies()).containsExactly("M", "Q", "S", "A");
    assertThat(properties.getChunkSize()).isEqualTo(1);
    assertThat(properties.getErrorThreshold()).isEqualTo(100);
  }

  @Test
  void rejectsInvalidFrequency() {
    BillingConfigProperties properties = new BillingConfigProperties();
    properties.setFrequencies(List.of("M", "X"));

    Set<ConstraintViolation<BillingConfigProperties>> violations = validator.validate(properties);
    assertThat(violations).isNotEmpty();
  }

  @Test
  void rejectsNegativeLeadDays() {
    BillingConfigProperties properties = new BillingConfigProperties();
    properties.setLeadDays(-1);

    Set<ConstraintViolation<BillingConfigProperties>> violations = validator.validate(properties);
    assertThat(violations).isNotEmpty();
  }
}
