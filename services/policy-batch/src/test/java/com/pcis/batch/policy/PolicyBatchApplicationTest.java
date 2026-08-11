package com.pcis.batch.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PolicyBatchApplicationTest {

  @Test
  void applicationClassLoads() {
    assertThat(PolicyBatchApplication.class).isNotNull();
  }
}
