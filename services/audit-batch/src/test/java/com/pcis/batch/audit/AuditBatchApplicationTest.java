package com.pcis.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditBatchApplicationTest {

  @Test
  void applicationClassLoads() {
    assertThat(AuditBatchApplication.class).isNotNull();
  }
}
