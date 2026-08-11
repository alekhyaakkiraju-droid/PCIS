package com.pcis.notification.application;

import com.pcis.notification.metrics.NotificationMetrics;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stub dispatcher that logs and records metrics with in-memory idempotency (WO-235). */
@Component
public class StubNotificationDispatcher implements NotificationDispatcher {

  private static final Logger log = LoggerFactory.getLogger(StubNotificationDispatcher.class);

  private final NotificationMetrics metrics;
  private final Set<UUID> processedKeys = ConcurrentHashMap.newKeySet();

  public StubNotificationDispatcher(NotificationMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public boolean dispatch(UUID idempotencyKey, String eventType, String payload) {
    if (!processedKeys.add(idempotencyKey)) {
      metrics.recordDuplicate();
      log.debug("Duplicate notification suppressed idempotencyKey={} eventType={}", idempotencyKey, eventType);
      return false;
    }

    metrics.recordDispatched(eventType);
    log.info(
        "Notification dispatched idempotencyKey={} eventType={} payloadBytes={}",
        idempotencyKey,
        eventType,
        payload == null ? 0 : payload.length());
    return true;
  }

  /** Visible for tests to reset idempotency state. */
  public void reset() {
    processedKeys.clear();
  }

  public int processedCount() {
    return processedKeys.size();
  }
}
