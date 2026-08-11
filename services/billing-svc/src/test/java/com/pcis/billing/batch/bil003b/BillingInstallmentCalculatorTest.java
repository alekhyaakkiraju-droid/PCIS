package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.bil003b.domain.BillingInstallmentCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingInstallmentCalculatorTest {

  @Test
  void monthlyInstallmentAmountMatchesGolden() {
    assertThat(BillingInstallmentCalculator.installmentAmount(new BigDecimal("600.00"), 12))
        .isEqualByComparingTo("50.00");
  }

  @Test
  void firstDueDateUsesReferenceDate() {
    LocalDate ref = LocalDate.parse("2024-06-15");
    assertThat(BillingInstallmentCalculator.nextDueDate(null, "M", ref)).isEqualTo(ref);
  }

  @Test
  void leadWindowIncludesZeroDaysOut() {
    assertThat(BillingInstallmentCalculator.withinLeadWindow(0, 15)).isTrue();
    assertThat(BillingInstallmentCalculator.withinLeadWindow(16, 15)).isFalse();
  }
}
