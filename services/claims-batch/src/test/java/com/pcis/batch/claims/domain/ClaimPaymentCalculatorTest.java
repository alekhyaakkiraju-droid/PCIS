package com.pcis.batch.claims.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClaimPaymentCalculatorTest {

  @Test
  void paymentAmountEqualsFullReserve() {
    assertThat(ClaimPaymentCalculator.paymentAmount(new BigDecimal("1500.00")))
        .isEqualByComparingTo("1500.00");
  }

  @Test
  void reinsuranceRecoveryRequiresStrictlyGreaterThanThreshold() {
    BigDecimal threshold = new BigDecimal("100000.00");
    assertThat(
            ClaimPaymentCalculator.requiresReinsuranceRecovery(new BigDecimal("100000.00"), threshold))
        .isFalse();
    assertThat(
            ClaimPaymentCalculator.requiresReinsuranceRecovery(new BigDecimal("100000.01"), threshold))
        .isTrue();
  }
}
