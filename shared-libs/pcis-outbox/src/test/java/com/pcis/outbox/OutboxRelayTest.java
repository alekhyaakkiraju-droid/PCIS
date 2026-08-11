package com.pcis.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.observability.metrics.OutboxMetrics;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

  @Mock private OutboxEventRepository repository;
  @Mock private KafkaOutboxEventPublisher publisher;
  @Mock private OutboxMetrics outboxMetrics;
  @Mock private ObjectProvider<OutboxMetrics> outboxMetricsProvider;

  private OutboxProperties properties;
  private OutboxRelay relay;

  @BeforeEach
  void setUp() {
    properties = new OutboxProperties();
    properties.setRelayBatchSize(10);
    properties.setRelayMaxRetries(3);
    properties.setRelayUser("TESTRLY");
    lenient()
        .doAnswer(
            invocation -> {
              Consumer<OutboxMetrics> consumer = invocation.getArgument(0);
              consumer.accept(outboxMetrics);
              return null;
            })
        .when(outboxMetricsProvider)
        .ifAvailable(any());

    relay = new OutboxRelay(repository, publisher, properties, outboxMetricsProvider);
  }

  @Test
  void relayPendingEvents_publishesAndMarksPublished() {
    OutboxEvent event = pendingEvent();
    when(repository.findPendingForRelaySkipLocked(10)).thenReturn(List.of(event));

    relay.relayPendingEvents();

    verify(publisher).publish(event);
    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(event.getUpdatedBy()).isEqualTo("TESTRLY");
    verify(repository).save(event);
    verify(outboxMetrics).refreshMetrics();
    verify(outboxMetrics).recordPublished(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void relaySingleEvent_movesToDeadLetterAndPublishesDlqAfterMaxRetries() {
    properties.setDlqKafkaTopic("audit-events-dlq");
    OutboxEvent event = pendingEvent();
    event.setAttemptCount(2);
    doThrow(new KafkaOutboxEventPublisher.OutboxPublishException("kafka down", new RuntimeException()))
        .when(publisher)
        .publish(event);

    relay.relaySingleEvent(event);

    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
    assertThat(event.getAttemptCount()).isEqualTo(3);
    verify(publisher).publishToDeadLetter(event, 3, event.getLastError());
    verify(outboxMetrics).recordDeadLetter();
  }

  @Test
  void relaySingleEvent_schedulesRetryOnPublishFailure() {
    OutboxEvent event = pendingEvent();
    doThrow(new KafkaOutboxEventPublisher.OutboxPublishException("kafka down", new RuntimeException()))
        .when(publisher)
        .publish(event);

    relay.relaySingleEvent(event);

    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(event.getAttemptCount()).isEqualTo(1);
    assertThat(event.getLastError()).contains("kafka down");
    assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
    verify(repository).save(event);
    verify(outboxMetrics).recordRelayError();
  }

  @Test
  void relaySingleEvent_movesToDeadLetterAfterMaxRetries() {
    OutboxEvent event = pendingEvent();
    event.setAttemptCount(2);
    doThrow(new KafkaOutboxEventPublisher.OutboxPublishException("still failing", new RuntimeException()))
        .when(publisher)
        .publish(event);

    relay.relaySingleEvent(event);

    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
    assertThat(event.getAttemptCount()).isEqualTo(3);
    verify(repository).save(event);
    verify(outboxMetrics).recordRelayError();
    verify(outboxMetrics).recordDeadLetter();
  }

  @Test
  void relayPendingEvents_noEventsSkipsPublish() {
    when(repository.findPendingForRelaySkipLocked(10)).thenReturn(List.of());

    relay.relayPendingEvents();

    verify(publisher, never()).publish(any());
    verify(outboxMetrics).refreshMetrics();
  }

  @Test
  void kafkaPublisher_sendsToConfiguredTopic() throws Exception {
    org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate =
        org.mockito.Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class);
    org.apache.kafka.clients.producer.RecordMetadata metadata =
        new org.apache.kafka.clients.producer.RecordMetadata(
            new org.apache.kafka.common.TopicPartition("pcis.test.events", 0), 0, 0, 0, 0, 0);
    org.springframework.kafka.support.SendResult<String, String> sendResult =
        new org.springframework.kafka.support.SendResult<>(
            new org.apache.kafka.clients.producer.ProducerRecord<>("pcis.test.events", "agg-1", "{}"),
            metadata);
    when(kafkaTemplate.send(any(org.apache.kafka.clients.producer.ProducerRecord.class)))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(sendResult));

    OutboxProperties props = new OutboxProperties();
    props.setKafkaTopic("pcis.test.events");
    KafkaOutboxEventPublisher kafkaPublisher =
        new KafkaOutboxEventPublisher(kafkaTemplate, new ObjectMapper(), props);

    OutboxEvent event = pendingEvent();
    kafkaPublisher.publish(event);

    ArgumentCaptor<org.apache.kafka.clients.producer.ProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(org.apache.kafka.clients.producer.ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    assertThat(captor.getValue().topic()).isEqualTo("pcis.test.events");
    assertThat(captor.getValue().key()).isEqualTo("agg-1");
  }

  private static OutboxEvent pendingEvent() {
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
    return event;
  }
}
