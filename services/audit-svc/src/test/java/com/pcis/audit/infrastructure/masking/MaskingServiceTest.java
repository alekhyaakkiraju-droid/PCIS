package com.pcis.audit.infrastructure.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingServiceTest {

  private final MaskingService maskingService = new MaskingService();

  @Test
  void masksSsnInOldValue() {
    assertThat(maskingService.mask("Customer tax id 123-45-6789 updated"))
        .contains("***-**-****")
        .doesNotContain("123-45-6789");
  }

  @Test
  void masksEmailInNewValue() {
    assertThat(maskingService.mask("email changed to alice@example.com"))
        .contains("@example.com")
        .doesNotContain("alice@");
  }

  @Test
  void passesThroughNullAndBlank() {
    assertThat(maskingService.mask(null)).isNull();
    assertThat(maskingService.mask("  ")).isEqualTo("  ");
  }

  @Test
  void leavesNonPiiUntouched() {
    assertThat(maskingService.mask("ACTIVE")).isEqualTo("ACTIVE");
  }
}
