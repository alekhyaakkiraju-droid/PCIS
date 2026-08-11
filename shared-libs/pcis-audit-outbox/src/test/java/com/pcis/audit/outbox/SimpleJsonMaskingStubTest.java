package com.pcis.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimpleJsonMaskingStubTest {

  private final SimpleJsonMaskingStub masker = new SimpleJsonMaskingStub();

  @Test
  void masksSsnEmailAndPhone() {
    assertThat(masker.mask("tax id 123-45-6789")).isEqualTo("tax id ***-**-****");
    assertThat(masker.mask("email alice@example.com")).isEqualTo("email ***@example.com");
    assertThat(masker.mask("phone 555-123-4567")).isEqualTo("phone ***-***-4567");
  }

  @Test
  void leavesNonPiiUnchanged() {
    assertThat(masker.mask("ACTIVE")).isEqualTo("ACTIVE");
    assertThat(masker.mask(null)).isNull();
  }
}
