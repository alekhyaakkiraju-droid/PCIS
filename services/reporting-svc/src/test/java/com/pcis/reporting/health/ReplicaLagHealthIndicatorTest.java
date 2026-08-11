package com.pcis.reporting.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pcis.reporting.metrics.ReplicaLagMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class ReplicaLagHealthIndicatorTest {
  @Test
  void downWhenLagHigh() {
    ReplicaLagMetrics metrics = mock(ReplicaLagMetrics.class);
    when(metrics.lagSecondsValue()).thenReturn(45.0);
    doAnswer(i -> null).when(metrics).refreshLag();
    assertThat(new ReplicaLagHealthIndicator(metrics).health().getStatus()).isEqualTo(Status.DOWN);
  }
}
