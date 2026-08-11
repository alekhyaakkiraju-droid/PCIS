package com.pcis.authz.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrincipalMaskingUtilTest {

  @Test
  void masksStandardPrincipalToLastFourCharacters() {
    assertThat(PrincipalMaskingUtil.maskPrincipal("ALICE")).isEqualTo("***LICE");
  }

  @Test
  void returnsShortPrincipalUnchanged() {
    assertThat(PrincipalMaskingUtil.maskPrincipal("AB")).isEqualTo("AB");
  }

  @Test
  void returnsMaskForNull() {
    assertThat(PrincipalMaskingUtil.maskPrincipal(null)).isEqualTo("***");
  }

  @Test
  void returnsMaskForBlank() {
    assertThat(PrincipalMaskingUtil.maskPrincipal("   ")).isEqualTo("***");
  }
}
