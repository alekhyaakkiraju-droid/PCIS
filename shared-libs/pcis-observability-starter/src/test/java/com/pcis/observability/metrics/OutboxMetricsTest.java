package com.pcis.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutboxMetricsTest {

  private OutboxEventMetricsRepository repository;
  private SimpleMeterRegistry registry;
  private OutboxMetrics metrics;

  @BeforeEach
  void setUp() {
    repository = mock(OutboxEventMetricsRepository.class);
    registry = new SimpleMeterRegistry();
    metrics = new OutboxMetrics(registry, repository, "audit-svc");
  }

  @Test
  void noPendingEvents_setsGaugesToZero() {
    when(repository.countPendingEvents()).thenReturn(0L);

    metrics.refreshMetrics();

    assertThat(metrics.pendingCountValue()).isZero();
    assertThat(metrics.lagSecondsValue()).isZero();
    assertThat(registry.find(OutboxMetrics.PENDING_METRIC).gauge().value()).isZero();
    assertThat(registry.find(OutboxMetrics.LAG_METRIC).gauge().value()).isZero();
  }

  @Test
  void pendingEvents_reflectsCountAndOldestLag() {
    Instant oldest = Instant.now().minusSeconds(45);
    when(repository.countPendingEvents()).thenReturn(5L);
    when(repository.oldestPendingEventCreatedAt()).thenReturn(Optional.of(oldest));

    metrics.refreshMetrics();

    assertThat(metrics.pendingCountValue()).isEqualTo(5.0);
    assertThat(metrics.lagSecondsValue()).isBetween(44.0, 46.0);
  }

  @Test
  void repositoryFailure_retainsLastKnownValues() {
    Instant oldest = Instant.now().minusSeconds(45);
    when(repository.countPendingEvents()).thenReturn(5L);
    when(repository.oldestPendingEventCreatedAt()).thenReturn(Optional.of(oldest));
    metrics.refreshMetrics();

    when(repository.countPendingEvents()).thenThrow(new RuntimeException("connection refused"));

    metrics.refreshMetrics();

    assertThat(metrics.pendingCountValue()).isEqualTo(5.0);
    assertThat(metrics.lagSecondsValue()).isBetween(44.0, 46.0);
  }

  @Test
  void registersRelayCountersAndTimer() {
    metrics.recordPublished(java.time.Duration.ofMillis(25));
    metrics.recordRelayError();
    metrics.recordDeadLetter();

    assertThat(registry.find(OutboxMetrics.PUBLISHED_METRIC).counter().count()).isEqualTo(1.0);
    assertThat(registry.find(OutboxMetrics.RELAYED_METRIC).counter().count()).isEqualTo(1.0);
    assertThat(registry.find(OutboxMetrics.RELAY_ERRORS_METRIC).counter().count()).isEqualTo(1.0);
    assertThat(registry.find(OutboxMetrics.DEAD_LETTER_METRIC).counter().count()).isEqualTo(1.0);
    assertThat(registry.find(OutboxMetrics.PUBLISH_DURATION_METRIC).timer().count()).isEqualTo(1L);
  }

  @Test
  void registersGaugesWithServiceTag() {
    assertThat(registry.find(OutboxMetrics.PENDING_METRIC).tags("service", "audit-svc").gauge())
        .isNotNull();
    assertThat(registry.find(OutboxMetrics.LAG_METRIC).tags("service", "audit-svc").gauge())
        .isNotNull();
  }
}
