package com.pcis.premium.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Immutable monetary value with scale 2 HALF_UP (WO-188). */
public final class MoneyAmount implements Serializable {

  private static final int SCALE = 2;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private final BigDecimal value;

  private MoneyAmount(BigDecimal value) {
    this.value = value.setScale(SCALE, ROUNDING);
  }

  public static MoneyAmount of(BigDecimal amount) {
    Objects.requireNonNull(amount, "amount is required");
    return new MoneyAmount(amount);
  }

  public BigDecimal value() {
    return value;
  }

  public DivisionResult divide(int divisor) {
    if (divisor <= 0) {
      throw new IllegalArgumentException("divisor must be positive: " + divisor);
    }
    BigDecimal perUnit = value.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING);
    BigDecimal remainder = value.subtract(perUnit.multiply(BigDecimal.valueOf(divisor)));
    return new DivisionResult(perUnit, remainder);
  }

  public record DivisionResult(BigDecimal perUnit, BigDecimal remainder) implements Serializable {}
}
