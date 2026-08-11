package com.pcis.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.notification.application.NotificationDispatcher;
import com.pcis.notification.config.NotificationKafkaProperties;
import com.pcis.notification.metrics.NotificationMetrics;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumes domain events relayed from transactional outbox publishers and dispatches notifications
 * with idempotent stub handling (WO-235).
 */
@Component
@ConditionalOnProperty(prefix = "pcis.notification.kafka", name = "consumer-enabled", havingValue = "true")
public class DomainEventNotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(DomainEventNotificationConsumer.class);

  private final NotificationDispatcher notificationDispatcher;
  private final NotificationMetrics metrics;
  private final NotificationKafkaProperties properties;
  private final ObjectMapper objectMapper;

  public DomainEventNotificationConsumer(
      NotificationDispatcher notificationDispatcher,
      NotificationMetrics metrics,
      NotificationKafkaProperties properties,
      ObjectMapper objectMapper) {
    this.notificationDispatcher = notificationDispatcher;
    this.metrics = metrics;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = "${pcis.outbox.kafka-topic:pcis.domain.events}",
      groupId = "${pcis.notification.kafka.consumer-group:reporting-svc-notification}")
  public void consume(ConsumerRecord<String, String> record) {
    String eventType = headerValue(record, "event-type");
    if (!isNotificationEvent(eventType, record.value())) {
      metrics.recordSkipped("not_notification");
      return;
    }

    UUID idempotencyKey = parseIdempotencyKey(record);
    if (idempotencyKey == null) {
      metrics.recordSkipped("missing_idempotency_key");
      log.warn("Skipping notification message without idempotency-key header offset={}", record.offset());
      return;
    }

    String resolvedEventType = resolveEventType(eventType, record.value());
    try {
      notificationDispatcher.dispatch(idempotencyKey, resolvedEventType, record.value());
    } catch (RuntimeException ex) {
      metrics.recordFailure();
      log.error(
          "Notification dispatch failed idempotencyKey={} offset={}",
          idempotencyKey,
          record.offset(),
          ex);
      throw ex;
    }
  }

  boolean isNotificationEvent(String eventType, String payload) {
    if (StringUtils.hasText(eventType)
        && eventType.startsWith(properties.notificationEventPrefix())) {
      return true;
    }
    return payload != null && payload.contains("\"notification\"");
  }

  private String resolveEventType(String headerEventType, String payload) {
    if (StringUtils.hasText(headerEventType)) {
      return headerEventType;
    }
    try {
      JsonNode node = objectMapper.readTree(payload);
      JsonNode notification = node.get("notification");
      if (notification != null && notification.hasNonNull("type")) {
        return notification.get("type").asText();
      }
    } catch (Exception ex) {
      log.debug("Unable to parse notification type from payload", ex);
    }
    return "NotificationEvent";
  }

  private static UUID parseIdempotencyKey(ConsumerRecord<String, String> record) {
    String raw = headerValue(record, "idempotency-key");
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    return UUID.fromString(raw);
  }

  private static String headerValue(ConsumerRecord<String, String> record, String name) {
    var header = record.headers().lastHeader(name);
    if (header == null || header.value() == null) {
      return null;
    }
    return new String(header.value(), StandardCharsets.UTF_8);
  }
}
