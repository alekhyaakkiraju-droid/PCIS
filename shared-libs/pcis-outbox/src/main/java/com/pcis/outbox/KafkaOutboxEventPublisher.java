package com.pcis.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.observability.MdcKeys;
import com.pcis.observability.propagation.ObservabilityHeaders;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.StringUtils;

public class KafkaOutboxEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaOutboxEventPublisher.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final OutboxProperties properties;

  public KafkaOutboxEventPublisher(
      KafkaTemplate<?, ?> kafkaTemplate,
      ObjectMapper objectMapper,
      OutboxProperties properties) {
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, String> typedTemplate = (KafkaTemplate<String, String>) kafkaTemplate;
    this.kafkaTemplate = typedTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public void publish(OutboxEvent event) {
    publishToTopic(properties.getKafkaTopic(), event, null, null);
  }

  public void publishToDeadLetter(OutboxEvent event, int retryCount, String errorReason) {
    if (!StringUtils.hasText(properties.getDlqKafkaTopic())) {
      return;
    }
    publishToTopic(properties.getDlqKafkaTopic(), event, retryCount, errorReason);
  }

  private void publishToTopic(
      String topic, OutboxEvent event, Integer retryCount, String errorReason) {
    String payloadJson = serializePayload(event);
    ProducerRecord<String, String> record =
        new ProducerRecord<>(topic, event.getAggregateId(), payloadJson);
    record
        .headers()
        .add(new RecordHeader("event-type", bytes(event.getEventType())))
        .add(new RecordHeader("aggregate-type", bytes(event.getAggregateType())))
        .add(new RecordHeader("idempotency-key", bytes(event.getIdempotencyKey().toString())));
    String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
    if (StringUtils.hasText(correlationId)) {
      record
          .headers()
          .add(
              new RecordHeader(
                  ObservabilityHeaders.KAFKA_CORRELATION_ID, bytes(correlationId)));
    }
    if (retryCount != null) {
      record
          .headers()
          .add(new RecordHeader("X-Original-Topic", bytes(properties.getKafkaTopic())))
          .add(new RecordHeader("X-Retry-Count", bytes(String.valueOf(retryCount))))
          .add(
              new RecordHeader(
                  "X-Error-Reason",
                  bytes(StringUtils.hasText(errorReason) ? errorReason : "publish failed")));
    }

    try {
      SendResult<String, String> result = kafkaTemplate.send(record).get();
      log.debug(
          "Published outbox event id={} to topic={} partition={} offset={}",
          event.getId(),
          topic,
          result.getRecordMetadata().partition(),
          result.getRecordMetadata().offset());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new OutboxPublishException("Interrupted while publishing outbox event " + event.getId(), ex);
    } catch (Exception ex) {
      throw new OutboxPublishException("Failed to publish outbox event " + event.getId(), ex);
    }
  }

  private String serializePayload(OutboxEvent event) {
    try {
      return objectMapper.writeValueAsString(event.getPayload());
    } catch (JsonProcessingException ex) {
      throw new OutboxPublishException(
          "Unable to serialize payload for outbox event " + event.getId(), ex);
    }
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  public static final class OutboxPublishException extends RuntimeException {
    public OutboxPublishException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
