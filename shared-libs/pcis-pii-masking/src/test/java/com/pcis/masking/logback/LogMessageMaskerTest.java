package com.pcis.masking.logback;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.masking.MaskingTestSupport;
import org.junit.jupiter.api.Test;

class LogMessageMaskerTest {

  @Test
  void maskIsIdempotentForAlreadyMaskedTaxId() {
    LogMessageMasker masker =
        new LogMessageMasker(MaskingTestSupport.maskingService(), MaskingTestSupport.registry());
    assertThat(masker.mask("6789")).isEqualTo("6789");
  }

  @Test
  void defaultRegistryMasksWithoutSpringContext() {
    LogMessageMasker masker = LogMessageMasker.withDefaultRegistry();
    assertThat(masker.mask("contact bob@example.com")).isEqualTo("contact example.com");
  }
}
