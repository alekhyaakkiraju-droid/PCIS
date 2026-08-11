package com.pcis.audit.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.audit.application.AuditEventService;
import com.pcis.audit.contract.AuditEventRequest;
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
 * Consumes audit events relayed from transactional outbox publishers with idempotent persistence.
 */
@Component
@ConditionalOnProperty(prefix = "pcis.audit.kafka", name = "consumer-enabled", havingValue = "true")
public class AuditOutboxEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AuditOutboxEventConsumer.class);

  private final AuditEventService auditEventService;
  private final ObjectMapper objectMapper;

  public AuditOutboxEventConsumer(AuditEventService auditEventService, ObjectMapper objectMapper) {
    this.auditEventService = auditEventService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = "${pcis.outbox.kafka-topic:pcis.domain.events}",
      groupId = "${pcis.audit.kafka.consumer-group:audit-svc-outbox-relay}")
  public void consume(ConsumerRecord<String, String> record) {
    String eventType = headerValue(record, "event-type");
    if (!isAuditEvent(eventType, record.value())) {
      return;
    }

    UUID idempotencyKey = parseIdempotencyKey(record);
    if (idempotencyKey == null) {
      log.warn("Skipping outbox relay message without idempotency-key header offset={}", record.offset());
      return;
    }

    AuditEventRequest request = toAuditEventRequest(record.value());
    auditEventService.recordEventIdempotent(idempotencyKey, request);
    log.debug(
        "Persisted relayed audit event idempotencyKey={} offset={}",
        idempotencyKey,
        record.offset());
  }

  static boolean isAuditEvent(String eventType, String payload) {
    if (StringUtils.hasText(eventType) && eventType.startsWith("Audit")) {
      return true;
    }
    return payload != null && payload.contains("\"action\"") && payload.contains("\"resource\"");
  }

  private AuditEventRequest toAuditEventRequest(String payloadJson) {
    try {
      JsonNode node = objectMapper.readTree(payloadJson);
      return new AuditEventRequest(
          text(node, "action"),
          text(node, "old_value"),
          text(node, "new_value"),
          text(node, "key"),
          text(node, "service"),
          text(node, "program"),
          text(node, "actor"),
          text(node, "resource"),
          text(node, "field_name"),
          uuid(node, "correlation_id"));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid audit outbox payload", ex);
    }
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

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private static UUID uuid(JsonNode node, String field) {
    String value = text(node, field);
    return value == null ? null : UUID.fromString(value);
  }
}
