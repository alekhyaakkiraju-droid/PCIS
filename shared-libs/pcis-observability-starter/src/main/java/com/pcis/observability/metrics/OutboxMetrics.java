package com.pcis.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Registers {@code pcis_audit_outbox_pending_count} and {@code pcis_audit_outbox_lag_seconds}
 * gauges for the audit outbox relay.
 *
 * <p>Call {@link #refreshMetrics()} at the end of each relay polling cycle. On query failure the
 * last known values are retained to avoid false-negative alert flapping.
 */
public class OutboxMetrics {

  private static final Logger log = LoggerFactory.getLogger(OutboxMetrics.class);

  static final String PENDING_METRIC = "pcis_audit_outbox_pending_count";
  static final String LAG_METRIC = "pcis_audit_outbox_lag_seconds";

  private final OutboxEventMetricsRepository repository;
  private final AtomicReference<Double> pendingCount = new AtomicReference<>(0.0);
  private final AtomicReference<Double> lagSeconds = new AtomicReference<>(0.0);

  public OutboxMetrics(
      MeterRegistry registry, OutboxEventMetricsRepository repository, String serviceName) {
    this.repository = repository;
    String service = StringUtils.hasText(serviceName) ? serviceName : "pcis-service";
    Gauge.builder(PENDING_METRIC, pendingCount, ref -> ref.get())
        .description("Unpublished outbox events awaiting relay")
        .tag("service", service)
        .register(registry);
    Gauge.builder(LAG_METRIC, lagSeconds, ref -> ref.get())
        .description("Age in seconds of the oldest unpublished outbox event")
        .tag("service", service)
        .register(registry);
  }

  /** Re-query pending count and lag from the repository and update gauge backing values. */
  public void refreshMetrics() {
    try {
      long pending = repository.countPendingEvents();
      pendingCount.set((double) pending);

      if (pending == 0) {
        lagSeconds.set(0.0);
        return;
      }

      Instant oldest = repository.oldestPendingEventCreatedAt().orElse(null);
      if (oldest == null) {
        lagSeconds.set(0.0);
        return;
      }

      double lag = Math.max(0.0, Duration.between(oldest, Instant.now()).toMillis() / 1000.0);
      lagSeconds.set(lag);
    } catch (RuntimeException ex) {
      log.warn(
          "Failed to refresh outbox metrics — retaining last known values (pending={}, lag={})",
          pendingCount.get(),
          lagSeconds.get(),
          ex);
    }
  }

  double pendingCountValue() {
    return pendingCount.get();
  }

  double lagSecondsValue() {
    return lagSeconds.get();
  }
}
