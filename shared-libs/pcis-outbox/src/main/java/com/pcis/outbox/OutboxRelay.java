package com.pcis.outbox;

import com.pcis.observability.metrics.OutboxMetrics;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

public class OutboxRelay {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

  private final OutboxEventRepository repository;
  private final KafkaOutboxEventPublisher publisher;
  private final OutboxProperties properties;
  private final ObjectProvider<OutboxMetrics> outboxMetrics;

  public OutboxRelay(
      OutboxEventRepository repository,
      KafkaOutboxEventPublisher publisher,
      OutboxProperties properties,
      ObjectProvider<OutboxMetrics> outboxMetrics) {
    this.repository = repository;
    this.publisher = publisher;
    this.properties = properties;
    this.outboxMetrics = outboxMetrics;
  }

  @Scheduled(fixedDelayString = "${pcis.outbox.relay-interval-ms:5000}")
  @Transactional
  public void relayPendingEvents() {
    var events = repository.findPendingForRelaySkipLocked(properties.getRelayBatchSize());
    if (events.isEmpty()) {
      refreshMetrics();
      return;
    }

    for (OutboxEvent event : events) {
      relaySingleEvent(event);
    }
    refreshMetrics();
  }

  @Transactional
  public void relaySingleEvent(OutboxEvent event) {
    try {
      publisher.publish(event);
      markPublished(event);
    } catch (KafkaOutboxEventPublisher.OutboxPublishException ex) {
      handlePublishFailure(event, ex);
    }
  }

  private void markPublished(OutboxEvent event) {
    event.setStatus(OutboxEventStatus.PUBLISHED);
    event.setUpdatedBy(properties.getRelayUser());
    event.setLastError(null);
    repository.save(event);
  }

  private void handlePublishFailure(OutboxEvent event, Exception ex) {
    int nextAttempt = event.getAttemptCount() + 1;
    event.setAttemptCount(nextAttempt);
    event.setLastError(truncate(ex.getMessage(), 500));
    event.setUpdatedBy(properties.getRelayUser());

    if (nextAttempt >= properties.getRelayMaxRetries()) {
      event.setStatus(OutboxEventStatus.DEAD_LETTER);
      log.error(
          "Outbox event id={} idempotencyKey={} moved to DEAD_LETTER after {} attempts: {}",
          event.getId(),
          event.getIdempotencyKey(),
          nextAttempt,
          ex.getMessage());
    } else {
      event.setNextAttemptAt(Instant.now().plus(backoffDuration(nextAttempt)));
      log.error(
          "Outbox event id={} publish failed (attempt {}/{}), scheduled retry at {}: {}",
          event.getId(),
          nextAttempt,
          properties.getRelayMaxRetries(),
          event.getNextAttemptAt(),
          ex.getMessage());
    }
    repository.save(event);
  }

  private Duration backoffDuration(int attemptCount) {
    long seconds = Math.min(300L, (long) Math.pow(2, attemptCount));
    return Duration.ofSeconds(seconds);
  }

  private void refreshMetrics() {
    outboxMetrics.ifAvailable(OutboxMetrics::refreshMetrics);
  }

  private static String truncate(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return "publish failed";
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
