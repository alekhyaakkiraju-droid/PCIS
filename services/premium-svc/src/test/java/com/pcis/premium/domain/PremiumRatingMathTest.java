package com.pcis.premium.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PremiumRatingMathTest {

  @Test
  void combineFactorsReturnsOneForEmptyList() {
    assertThat(PremiumRatingMath.combineFactors(List.of())).isEqualByComparingTo("1");
  }

  @Test
  void combineFactorsMultipliesAllValues() {
    var combined =
        PremiumRatingMath.combineFactors(
            List.of(new BigDecimal("1.0500"), new BigDecimal("1.0200"), new BigDecimal("1.0100")));
    assertThat(combined).isEqualByComparingTo("1.081710");
  }

  @Test
  void multiplyAndRoundUsesHalfUpAtHalfCentBoundary() {
    var result =
        PremiumRatingMath.multiplyAndRound(new BigDecimal("10.005"), BigDecimal.ONE, 2);
    assertThat(result).isEqualByComparingTo("10.01");
  }

  @Test
  void multiplyAndRoundPreservesExactCentValues() {
    var result =
        PremiumRatingMath.multiplyAndRound(new BigDecimal("1200.00"), new BigDecimal("1.0500"), 2);
    assertThat(result).isEqualByComparingTo("1260.00");
  }
}
