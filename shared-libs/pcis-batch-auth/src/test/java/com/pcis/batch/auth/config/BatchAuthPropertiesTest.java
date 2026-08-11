package com.pcis.batch.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BatchAuthPropertiesTest {

  @Test
  void defaultExpirationBufferIsThirtySeconds() {
    BatchAuthProperties properties = new BatchAuthProperties();
    assertThat(properties.getExpirationBufferSeconds()).isEqualTo(30);
  }

  @Test
  void rejectsNegativeExpirationBuffer() {
    BatchAuthProperties properties = new BatchAuthProperties();
    assertThatThrownBy(() -> properties.setExpirationBufferSeconds(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
