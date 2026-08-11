package com.pcis.authz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthzApplicationTest {

  @Test
  void mainClassIsDiscoverable() {
    assertThat(AuthzApplication.class.getSimpleName()).isEqualTo("AuthzApplication");
  }
}
