package com.pcis.authz.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditorAwareImplTest {

  @Test
  void returnsSystemAuditor() {
    var auditorAware = new AuditorAwareImpl();
    assertThat(auditorAware.getCurrentAuditor()).contains("SYSTEM");
  }
}
