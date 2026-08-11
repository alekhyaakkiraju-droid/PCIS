package com.pcis.notification.application;

import java.util.UUID;

/** Dispatches notification actions for consumed domain events (WO-235). */
public interface NotificationDispatcher {

  /**
   * @return true when the event was dispatched, false when suppressed as duplicate
   */
  boolean dispatch(UUID idempotencyKey, String eventType, String payload);
}
