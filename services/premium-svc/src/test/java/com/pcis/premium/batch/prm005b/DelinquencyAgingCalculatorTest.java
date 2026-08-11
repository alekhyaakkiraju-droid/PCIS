package com.pcis.premium.batch.prm005b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.premium.batch.prm005b.domain.DelinquencyAgingCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DelinquencyAgingCalculatorTest {

  @ParameterizedTest
  @CsvSource({
    "500, 500, 5, 10, P",
    "500, 0, 5, 10, D",
    "500, 0, 10, 10, D",
    "500, 0, 11, 10, L"
  })
  void statusTransitionMatrix(
      String amtDue, String amtPaid, int daysPastDue, int graceDays, String expected) {
    assertThat(
            DelinquencyAgingCalculator.computeStatus(
                new BigDecimal(amtDue),
                new BigDecimal(amtPaid),
                daysPastDue,
                graceDays))
        .isEqualTo(expected);
  }
}
