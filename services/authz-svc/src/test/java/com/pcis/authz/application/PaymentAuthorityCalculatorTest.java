package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentAuthorityCalculatorTest {

  @Test
  void cumulativePayoutUsesHalfUpAtScaleTwo() {
    BigDecimal cumulative =
        PaymentAuthorityCalculator.cumulativePayout(new BigDecimal("20000.005"), new BigDecimal("5000.004"));

    assertThat(cumulative).isEqualByComparingTo("25000.01");
  }

  @Test
  void exceedsAuthorityLimitWhenCumulativeIsGreater() {
    assertThat(
            PaymentAuthorityCalculator.exceedsAuthorityLimit(
                new BigDecimal("25000.01"), new BigDecimal("25000.00")))
        .isTrue();
  }

  @Test
  void doesNotExceedAuthorityLimitWhenExactlyEqual() {
    assertThat(
            PaymentAuthorityCalculator.exceedsAuthorityLimit(
                new BigDecimal("25000.00"), new BigDecimal("25000.00")))
        .isFalse();
  }
}
