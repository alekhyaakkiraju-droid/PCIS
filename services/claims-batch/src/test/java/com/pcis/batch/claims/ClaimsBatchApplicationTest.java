package com.pcis.batch.claims;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaimsBatchApplicationTest {

  @Test
  void applicationClassLoads() {
    assertThat(ClaimsBatchApplication.class).isNotNull();
  }
}
