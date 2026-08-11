package com.pcis.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

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
    String payloadJson = serializePayload(event);
    ProducerRecord<String, String> record =
        new ProducerRecord<>(properties.getKafkaTopic(), event.getAggregateId(), payloadJson);
    record
        .headers()
        .add(new RecordHeader("event-type", bytes(event.getEventType())))
        .add(new RecordHeader("aggregate-type", bytes(event.getAggregateType())))
        .add(new RecordHeader("idempotency-key", bytes(event.getIdempotencyKey().toString())));

    try {
      SendResult<String, String> result = kafkaTemplate.send(record).get();
      log.debug(
          "Published outbox event id={} to partition={} offset={}",
          event.getId(),
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
