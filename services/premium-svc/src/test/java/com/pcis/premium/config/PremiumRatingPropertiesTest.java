package com.pcis.premium.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PremiumRatingPropertiesTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void bindsValidProperties() {
    PremiumRatingProperties properties = new PremiumRatingProperties();
    properties.setReferralThreshold(new BigDecimal("50000.00"));
    properties.setMaxCoverageLinesPerRequest(10);

    Set<ConstraintViolation<PremiumRatingProperties>> violations = validator.validate(properties);
    assertThat(violations).isEmpty();
    assertThat(properties.getReferralThreshold()).isEqualByComparingTo("50000.00");
  }

  @Test
  void rejectsOutOfRangeReferralThreshold() {
    PremiumRatingProperties properties = new PremiumRatingProperties();
    properties.setReferralThreshold(new BigDecimal("-1.00"));

    Set<ConstraintViolation<PremiumRatingProperties>> violations = validator.validate(properties);
    assertThat(violations).isNotEmpty();
  }
}
