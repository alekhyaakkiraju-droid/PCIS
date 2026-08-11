package com.pcis.claims.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.claims.ClaimsSvcApplication;
import com.pcis.claims.support.ClaimsTestSecurityConfig;
import com.pcis.claims.support.PostgresTestContainer;
import com.pcis.outbox.OutboxEvent;
import com.pcis.outbox.OutboxEventRepository;
import com.pcis.outbox.OutboxEventStatus;
import com.pcis.outbox.OutboxRelay;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    classes = ClaimsSvcApplication.class,
    properties = {
      "pcis.outbox.relay-enabled=true",
      "spring.task.scheduling.enabled=false",
      "pcis.outbox.kafka-topic=claims-events",
      "pcis.outbox.dlq-kafka-topic=claims-events-dlq",
      "pcis.outbox.metrics-namespace=claims_outbox",
      "pcis.outbox.relay-max-retries=3",
      "management.endpoint.health.probes.enabled=false"
    })
@EmbeddedKafka(
    partitions = 1,
    topics = {"claims-events", "claims-events-dlq"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClaimsTestSecurityConfig.class)
@EnabledIf("com.pcis.claims.support.TestEnvironment#isDockerAvailable")
class ClaimsOutboxRelayIntegrationTest {

  @Autowired private OutboxEventRepository repository;
  @Autowired private OutboxRelay outboxRelay;
  @Autowired private EmbeddedKafkaBroker embeddedKafka;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @BeforeEach
  void cleanOutbox() {
    repository.deleteAll();
  }

  @Test
  void relayPublishesFiveEventsInCreatedOrder() {
    List<Long> savedIds = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      OutboxEvent saved =
          repository.saveAndFlush(
              pendingEvent(
                  "CLM00000010" + i, "ClaimCreated", Instant.parse("2026-01-0" + (i + 1) + "T00:00:00Z")));
      savedIds.add(saved.getId());
    }

    try (Consumer<String, String> consumer = createConsumer("claims-events-" + UUID.randomUUID())) {
      embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "claims-events");

      for (Long id : savedIds) {
        outboxRelay.relaySingleEvent(repository.findById(id).orElseThrow());
      }
      repository.flush();

      ConsumerRecords<String, String> records =
          KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 5);
      assertThat(records.count()).isEqualTo(5);

      var iterator = records.records("claims-events").iterator();
      for (int i = 0; i < 5; i++) {
        var record = iterator.next();
        assertThat(record.key()).isEqualTo("CLM00000010" + i);
        assertThat(record.value()).contains("CLM00000010" + i);
        assertThat(
                new String(
                    record.headers().lastHeader("event-type").value(), java.nio.charset.StandardCharsets.UTF_8))
            .isEqualTo("ClaimCreated");
      }
    }

    assertThat(
            savedIds.stream()
                .map(id -> repository.findById(id).orElseThrow())
                .toList())
        .allMatch(event -> event.getStatus() == OutboxEventStatus.PUBLISHED);
  }

  private OutboxEvent pendingEvent(String claimNbr, String eventType, Instant createdAt) {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType("Claim");
    event.setAggregateId(claimNbr);
    event.setEventType(eventType);
    event.setPayload(Map.of("claimNbr", claimNbr));
    event.setIdempotencyKey(UUID.randomUUID());
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedAt(createdAt);
    event.setNextAttemptAt(Instant.parse("2020-01-01T00:00:00Z"));
    event.setCreatedBy("ITTEST");
    return event;
  }

  private Consumer<String, String> createConsumer(String topic) {
    var props =
        KafkaTestUtils.consumerProps(
            "claims-outbox-it-" + UUID.randomUUID(), "true", embeddedKafka);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    Consumer<String, String> consumer =
        new org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, String>(props)
            .createConsumer();
    consumer.subscribe(List.of(topic));
    return consumer;
  }
}
