package com.pcis.observability.metrics;

import java.time.Instant;
import java.util.Optional;

/**
 * Read-only access to outbox relay health for Micrometer gauges.
 *
 * <p>Domain services implement this against their {@code outbox_events} table. Queries should be
 * lightweight (single round-trip per relay cycle).
 */
public interface OutboxEventMetricsRepository {

  /** Count of unpublished (pending relay) outbox events. */
  long countPendingEvents();

  /** Creation time of the oldest pending event, if any. */
  Optional<Instant> oldestPendingEventCreatedAt();
}
