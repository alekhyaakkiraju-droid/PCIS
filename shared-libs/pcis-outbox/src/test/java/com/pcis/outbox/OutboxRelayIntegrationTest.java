package com.pcis.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.observability.metrics.OutboxMetrics;
import com.pcis.outbox.support.OutboxTestApplication;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end relay test with PostgreSQL and embedded Kafka.
 *
 * <p>Skipped when Docker is unavailable.
 */
@EnabledIf("dockerAvailable")
@Testcontainers(disabledWithoutDocker = true)
@EmbeddedKafka(
    partitions = 1,
    topics = {"pcis.domain.events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@SpringBootTest(classes = {OutboxTestApplication.class, OutboxRelayIntegrationTest.IntegrationConfig.class})
@ImportAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@ActiveProfiles("test")
class OutboxRelayIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("pcis_outbox_it")
          .withUsername("pcis")
          .withPassword("pcis");

  @Autowired private OutboxEventRepository repository;
  @Autowired private OutboxRelay outboxRelay;
  @Autowired private EmbeddedKafkaBroker embeddedKafka;

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("pcis.outbox.relay-enabled", () -> "false");
  }

  @BeforeEach
  void resetSchema(@Autowired javax.sql.DataSource dataSource) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/outbox_events.sql"));
    populator.execute(dataSource);
  }

  @Test
  void relayPublishesPendingEventToKafkaAndMarksPublished() {
    UUID idempotencyKey = UUID.fromString("cccccccc-dddd-eeee-ffff-111111111111");
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType("Claim");
    event.setAggregateId("CLM-IT-1");
    event.setEventType("ClaimCreated");
    event.setPayload(Map.of("amount", 250));
    event.setIdempotencyKey(idempotencyKey);
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.parse("2020-01-01T00:00:00Z"));
    event.setCreatedBy("ITTEST");
    OutboxEvent saved = repository.saveAndFlush(event);

    try (Consumer<String, String> consumer = createConsumer()) {
      embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "pcis.domain.events");

      outboxRelay.relaySingleEvent(repository.findById(saved.getId()).orElseThrow());
      repository.flush();

      OutboxEvent updated = repository.findById(saved.getId()).orElseThrow();
      assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
      assertThat(updated.isPublished()).isTrue();
      assertThat(updated.getAttemptCount()).isZero();

      ConsumerRecords<String, String> records =
          KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5), 1);
      assertThat(records.count()).isEqualTo(1);
      var record = records.iterator().next();
      assertThat(record.key()).isEqualTo("CLM-IT-1");
      assertThat(record.value()).contains("\"amount\":250");
      assertThat(new String(record.headers().lastHeader("event-type").value(), StandardCharsets.UTF_8))
          .isEqualTo("ClaimCreated");
    }
  }

  @Test
  void findByPublishedFalseOrderByCreatedAtAsc_returnsOnlyUnpublished() {
    OutboxEvent pending = buildEvent("PENDING", Instant.parse("2026-01-01T00:00:00Z"));
    OutboxEvent published = buildEvent("PUBLISHED", Instant.parse("2025-12-31T00:00:00Z"));
    repository.saveAll(java.util.List.of(pending, published));

    var unpublished =
        repository.findByPublishedFalseOrderByCreatedAtAsc(
            org.springframework.data.domain.PageRequest.of(0, 10));

    assertThat(unpublished).hasSize(1);
    assertThat(unpublished.getFirst().getEventType()).isEqualTo("PendingEvent");
  }

  private OutboxEvent buildEvent(String status, Instant createdAt) {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType("Billing");
    event.setAggregateId("BIL-" + status);
    event.setEventType(status.equals("PENDING") ? "PendingEvent" : "PublishedEvent");
    event.setPayload(Map.of("status", status));
    event.setIdempotencyKey(UUID.randomUUID());
    event.setStatus(OutboxEventStatus.valueOf(status));
    event.setAttemptCount(0);
    event.setCreatedAt(createdAt);
    event.setNextAttemptAt(createdAt);
    return event;
  }

  private Consumer<String, String> createConsumer() {
    var props = KafkaTestUtils.consumerProps("pcis-outbox-it-" + UUID.randomUUID(), "true", embeddedKafka);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, String>(props)
        .createConsumer();
  }

  @TestConfiguration
  @Import(OutboxAutoConfiguration.class)
  static class IntegrationConfig {

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(EmbeddedKafkaBroker embeddedKafka) {
      var props = KafkaTestUtils.producerProps(embeddedKafka);
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10_000);
      return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    KafkaOutboxEventPublisher kafkaOutboxEventPublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        com.fasterxml.jackson.databind.ObjectMapper objectMapper,
        OutboxProperties properties) {
      return new KafkaOutboxEventPublisher(kafkaTemplate, objectMapper, properties);
    }

    @Bean
    OutboxRelay outboxRelay(
        OutboxEventRepository repository,
        KafkaOutboxEventPublisher publisher,
        OutboxProperties properties,
        org.springframework.beans.factory.ObjectProvider<OutboxMetrics> outboxMetrics) {
      return new OutboxRelay(repository, publisher, properties, outboxMetrics);
    }
  }
}
