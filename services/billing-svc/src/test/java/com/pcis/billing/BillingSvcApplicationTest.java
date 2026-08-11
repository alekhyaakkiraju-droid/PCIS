package com.pcis.billing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BillingSvcApplicationTest {

  @Test
  void applicationClassLoads() {
    assertThat(BillingSvcApplication.class).isNotNull();
  }
}
