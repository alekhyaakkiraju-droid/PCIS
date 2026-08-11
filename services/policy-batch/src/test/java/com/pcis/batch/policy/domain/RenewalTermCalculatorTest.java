package com.pcis.batch.policy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RenewalTermCalculatorTest {

  @Test
  void computesRenewalTermDates() {
    LocalDate eff = LocalDate.of(2025, 1, 1);
    LocalDate exp = LocalDate.of(2026, 1, 1);

    assertThat(RenewalTermCalculator.newEffectiveDate(exp)).isEqualTo(LocalDate.of(2026, 1, 2));
    assertThat(RenewalTermCalculator.newExpirationDate(eff, exp))
        .isEqualTo(LocalDate.of(2027, 1, 2));
  }

  @Test
  void derivesRenewalPolicyNumberWithinTwelveChars() {
    assertThat(RenewalTermCalculator.renewalPolicyNumber("POL00010001"))
        .isEqualTo("POL00010001R");
  }
}
