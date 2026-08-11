package com.pcis.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.observability.MdcKeys;
import com.pcis.observability.propagation.ObservabilityHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaOutboxEventPublisherCorrelationTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void publish_addsPcisCorrelationIdHeaderFromMdc() throws Exception {
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    RecordMetadata metadata =
        new RecordMetadata(new TopicPartition("pcis.test.events", 0), 0, 0, 0, 0, 0);
    when(kafkaTemplate.send(any(ProducerRecord.class)))
        .thenReturn(
            java.util.concurrent.CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("pcis.test.events", "agg-1", "{}"), metadata)));

    OutboxProperties props = new OutboxProperties();
    props.setKafkaTopic("pcis.test.events");
    KafkaOutboxEventPublisher publisher =
        new KafkaOutboxEventPublisher(kafkaTemplate, new ObjectMapper(), props);

    MDC.put(MdcKeys.CORRELATION_ID, "corr-kafka-123");

    OutboxEvent event = new OutboxEvent();
    event.setId(1L);
    event.setAggregateType("Claim");
    event.setAggregateId("agg-1");
    event.setEventType("ClaimCreated");
    event.setPayload(Map.of("amount", 100));
    event.setIdempotencyKey(UUID.randomUUID());
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.now());

    publisher.publish(event);

    ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    byte[] headerValue =
        captor.getValue().headers().lastHeader(ObservabilityHeaders.KAFKA_CORRELATION_ID).value();
    assertThat(new String(headerValue, StandardCharsets.UTF_8)).isEqualTo("corr-kafka-123");
  }

  @Test
  void publish_omitsCorrelationHeaderWhenMdcEmpty() throws Exception {
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    RecordMetadata metadata =
        new RecordMetadata(new TopicPartition("pcis.test.events", 0), 0, 0, 0, 0, 0);
    when(kafkaTemplate.send(any(ProducerRecord.class)))
        .thenReturn(
            java.util.concurrent.CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("pcis.test.events", "agg-1", "{}"), metadata)));

    OutboxProperties props = new OutboxProperties();
    props.setKafkaTopic("pcis.test.events");
    KafkaOutboxEventPublisher publisher =
        new KafkaOutboxEventPublisher(kafkaTemplate, new ObjectMapper(), props);

    OutboxEvent event = new OutboxEvent();
    event.setId(2L);
    event.setAggregateType("Claim");
    event.setAggregateId("agg-2");
    event.setEventType("ClaimCreated");
    event.setPayload(Map.of("amount", 50));
    event.setIdempotencyKey(UUID.randomUUID());
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.now());

    publisher.publish(event);

    ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    assertThat(captor.getValue().headers().lastHeader(ObservabilityHeaders.KAFKA_CORRELATION_ID))
        .isNull();
  }
}
