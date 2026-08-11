package com.pcis.billing.batch.cmm001b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.cmm001b.domain.CommissionCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommissionCalculatorTest {

  @Test
  void commissionMatchesGoldenScenario() {
    assertThat(
            CommissionCalculator.commissionAmount(new BigDecimal("1000.00"), new BigDecimal("10.0000")))
        .isEqualByComparingTo("100.00");
  }
}
