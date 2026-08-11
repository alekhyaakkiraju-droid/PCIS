package com.pcis.premium.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyAmountTest {

  @Test
  void enforcesScaleTwoOnConstruction() {
    assertThat(MoneyAmount.of(new BigDecimal("10.005")).value())
        .isEqualByComparingTo("10.01");
  }

  @Test
  void divideReturnsPerUnitAndRemainder() {
    MoneyAmount.DivisionResult result = MoneyAmount.of(new BigDecimal("1000.00")).divide(3);
    assertThat(result.perUnit()).isEqualByComparingTo("333.33");
    assertThat(result.remainder()).isEqualByComparingTo("0.01");
  }

  @Test
  void divideRejectsNonPositiveDivisor() {
    assertThatThrownBy(() -> MoneyAmount.of(new BigDecimal("10.00")).divide(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void valueObjectIsImmutable() {
    MoneyAmount first = MoneyAmount.of(new BigDecimal("12.34"));
    MoneyAmount second = MoneyAmount.of(new BigDecimal("12.34"));
    assertThat(first.value()).isEqualByComparingTo(second.value());
  }
}
