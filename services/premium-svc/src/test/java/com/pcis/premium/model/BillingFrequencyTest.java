package com.pcis.premium.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BillingFrequencyTest {

  @Test
  void mapsLegacyCodesToInstallmentCounts() {
    assertThat(BillingFrequency.M.getInstallmentCount()).isEqualTo(12);
    assertThat(BillingFrequency.Q.getInstallmentCount()).isEqualTo(4);
    assertThat(BillingFrequency.S.getInstallmentCount()).isEqualTo(2);
    assertThat(BillingFrequency.A.getInstallmentCount()).isEqualTo(1);
  }

  @Test
  void fromCodeIsCaseInsensitive() {
    assertThat(BillingFrequency.fromCode("m")).isEqualTo(BillingFrequency.M);
  }

  @Test
  void unknownCodeDefaultsToAnnual() {
    assertThat(BillingFrequency.fromCode("X")).isEqualTo(BillingFrequency.A);
    assertThat(BillingFrequency.fromCode(null)).isEqualTo(BillingFrequency.A);
  }
}
