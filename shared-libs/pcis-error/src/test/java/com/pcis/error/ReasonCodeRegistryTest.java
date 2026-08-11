package com.pcis.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ReasonCodeRegistryTest {

  @Test
  void allReasonCodesAreUniqueWithClientSafeTitles() {
    Set<String> codes =
        java.util.Arrays.stream(ReasonCode.values())
            .map(ReasonCode::code)
            .collect(Collectors.toSet());
    assertThat(codes).hasSize(ReasonCode.values().length);
    for (ReasonCode reason : ReasonCode.values()) {
      assertThat(reason.title()).isNotBlank();
      assertThat(reason.type()).isNotNull();
    }
  }

  @Test
  void requireResolvesRegisteredCode() {
    assertThat(ReasonCodeRegistry.require("PRM_NOT_IMPLEMENTED"))
        .isEqualTo(ReasonCode.PRM_NOT_IMPLEMENTED);
  }

  @Test
  void requireRejectsUnknownCode() {
    assertThatThrownBy(() -> ReasonCodeRegistry.require("NOT_A_CODE"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
