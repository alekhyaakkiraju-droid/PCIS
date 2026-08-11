package com.pcis.notification.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class NotificationMetricsTest {

  @Test
  void recordsDispatchedSkippedDuplicateAndFailureCounters() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    NotificationMetrics metrics = new NotificationMetrics(registry);

    metrics.recordDispatched("NotificationClaimStatusChanged");
    metrics.recordSkipped("not_notification");
    metrics.recordDuplicate();
    metrics.recordFailure();

    assertThat(registry.find(NotificationMetrics.DISPATCHED).counters()).hasSize(1);
    assertThat(registry.find(NotificationMetrics.SKIPPED).counter()).isNotNull();
    assertThat(registry.find(NotificationMetrics.DUPLICATES).counter().count()).isEqualTo(1.0);
    assertThat(registry.find(NotificationMetrics.FAILURES).counter().count()).isEqualTo(1.0);
  }
}
