package com.pcis.observability.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ObservabilityPropertiesTest {

  @Test
  void defaultsAndValidation() {
    ObservabilityProperties properties = new ObservabilityProperties();
    assertThat(properties.getTraceSampleRate()).isEqualTo(0.1d);
    assertThat(properties.getCorrelationHeader()).isEqualTo("X-Correlation-ID");

    properties.setTraceSampleRate(1.0d);
    assertThat(properties.getTraceSampleRate()).isEqualTo(1.0d);

    assertThatThrownBy(() -> properties.setTraceSampleRate(1.1d))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties.setTraceSampleRate(-0.01d))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
