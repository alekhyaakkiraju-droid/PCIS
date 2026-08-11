package com.pcis.batch.claims.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClaimPaymentCalculatorTest {

  @Test
  void outstandingAmountUsesHalfUpScale2() {
    assertThat(
            ClaimPaymentCalculator.outstandingAmount(
                new BigDecimal("1500.005"), BigDecimal.ZERO))
        .isEqualByComparingTo("1500.01");
  }

  @Test
  void requiresRecoveryWhenStrictlyAboveThreshold() {
    assertThat(
            ClaimPaymentCalculator.requiresReinsuranceRecovery(
                new BigDecimal("100000.00"), new BigDecimal("100000.00")))
        .isFalse();
    assertThat(
            ClaimPaymentCalculator.requiresReinsuranceRecovery(
                new BigDecimal("100000.01"), new BigDecimal("100000.00")))
        .isTrue();
  }

  @Test
  void paymentAmountEqualsOutstanding() {
    assertThat(
            ClaimPaymentCalculator.paymentAmount(
                new BigDecimal("5000.00"), new BigDecimal("1250.50")))
        .isEqualByComparingTo("3749.50");
  }
}
