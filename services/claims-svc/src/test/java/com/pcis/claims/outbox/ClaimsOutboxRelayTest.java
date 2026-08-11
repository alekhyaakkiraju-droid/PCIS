package com.pcis.claims.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.observability.metrics.OutboxMetrics;
import com.pcis.outbox.KafkaOutboxEventPublisher;
import com.pcis.outbox.OutboxEvent;
import com.pcis.outbox.OutboxEventRepository;
import com.pcis.outbox.OutboxEventStatus;
import com.pcis.outbox.OutboxProperties;
import com.pcis.outbox.OutboxRelay;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ClaimsOutboxRelayTest {

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
    properties.setKafkaTopic("claims-events");
    properties.setDlqKafkaTopic("claims-events-dlq");
    properties.setMetricsNamespace("claims_outbox");
    org.mockito.Mockito.lenient()
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
  void routesToDlqAfterMaxRetries() {
    OutboxEvent event = pendingEvent();
    event.setAttemptCount(2);
    doThrow(new KafkaOutboxEventPublisher.OutboxPublishException("kafka down", new RuntimeException()))
        .when(publisher)
        .publish(event);

    relay.relaySingleEvent(event);

    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
    verify(publisher).publishToDeadLetter(eq(event), eq(3), any());
    verify(outboxMetrics).recordRelayError();
    verify(outboxMetrics).recordDeadLetter();
  }

  @Test
  void publishesPendingEventsInBatchOrder() {
    OutboxEvent first = pendingEvent();
    OutboxEvent second = pendingEvent();
    when(repository.findPendingForRelaySkipLocked(10)).thenReturn(List.of(first, second));

    relay.relayPendingEvents();

    verify(publisher).publish(first);
    verify(publisher).publish(second);
    assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(second.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
  }

  private static OutboxEvent pendingEvent() {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType("Claim");
    event.setAggregateId("CLM000000001");
    event.setEventType("PaymentDisbursed");
    event.setPayload(Map.of("paymentAmt", "1500.00"));
    event.setIdempotencyKey(UUID.randomUUID());
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.now());
    return event;
  }
}
