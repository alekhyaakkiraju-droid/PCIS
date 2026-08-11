package com.pcis.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Prometheus metrics for domain-event notification dispatch (WO-235/WO-236). */
@Component
public class NotificationMetrics {

  public static final String DISPATCHED = "notification_events_dispatched_total";
  public static final String SKIPPED = "notification_events_skipped_total";
  public static final String DUPLICATES = "notification_events_duplicate_total";
  public static final String FAILURES = "notification_events_failed_total";

  private final MeterRegistry registry;
  private final Map<String, Counter> dispatchedCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> skippedCounters = new ConcurrentHashMap<>();
  private final Counter duplicateCounter;
  private final Counter failureCounter;

  public NotificationMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.duplicateCounter =
        Counter.builder(DUPLICATES)
            .description("Duplicate notification events suppressed by idempotency")
            .tag("service", "reporting-svc")
            .register(registry);
    this.failureCounter =
        Counter.builder(FAILURES)
            .description("Notification dispatch failures")
            .tag("service", "reporting-svc")
            .register(registry);
  }

  public void recordDispatched(String eventType) {
    dispatchedCounter(eventType).increment();
  }

  public void recordSkipped(String reason) {
    skippedCounter(reason).increment();
  }

  public void recordDuplicate() {
    duplicateCounter.increment();
  }

  public void recordFailure() {
    failureCounter.increment();
  }

  private Counter dispatchedCounter(String eventType) {
    return dispatchedCounters.computeIfAbsent(
        eventType,
        type ->
            Counter.builder(DISPATCHED)
                .description("Notification events dispatched")
                .tag("service", "reporting-svc")
                .tag("event_type", type)
                .register(registry));
  }

  private Counter skippedCounter(String reason) {
    return skippedCounters.computeIfAbsent(
        reason,
        skipReason ->
            Counter.builder(SKIPPED)
                .description("Notification events skipped")
                .tag("service", "reporting-svc")
                .tag("reason", skipReason)
                .register(registry));
  }
}
