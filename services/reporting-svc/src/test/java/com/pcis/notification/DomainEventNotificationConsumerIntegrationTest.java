package com.pcis.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.pcis.notification.application.StubNotificationDispatcher;
import com.pcis.reporting.support.TestEnvironment;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = com.pcis.reporting.ReportingApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"pcis.domain.events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@TestPropertySource(
    properties = {
      "pcis.notification.kafka.consumer-enabled=true",
      "pcis.outbox.kafka-topic=pcis.domain.events",
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
    })
@EnabledIf("com.pcis.reporting.support.TestEnvironment#isDockerAvailable")
class DomainEventNotificationConsumerIntegrationTest {

  @Autowired private StubNotificationDispatcher notificationDispatcher;
  @Autowired private EmbeddedKafkaBroker embeddedKafka;

  @BeforeEach
  void resetDispatcher() {
    notificationDispatcher.reset();
  }

  @Test
  void duplicateNotificationEventsDispatchedExactlyOnce() {
    UUID idempotencyKey = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-222222222222");
    String payload =
        """
        {
          "notification": {
            "type": "ClaimStatusChanged",
            "recipient": "agent001",
            "channel": "email"
          },
          "claimNbr": "CLM000000001"
        }
        """;

    KafkaTemplate<String, String> producer = createProducer();
    ProducerRecord<String, String> record =
        new ProducerRecord<>("pcis.domain.events", "CLM000000001", payload);
    record
        .headers()
        .add(new RecordHeader("event-type", bytes("NotificationClaimStatusChanged")))
        .add(new RecordHeader("idempotency-key", bytes(idempotencyKey.toString())));

    producer.send(record);
    producer.send(record);
    producer.flush();

    await()
        .atMost(Duration.ofSeconds(30))
        .pollDelay(Duration.ofMillis(500))
        .untilAsserted(() -> assertThat(notificationDispatcher.processedCount()).isEqualTo(1));
  }

  @Test
  void nonNotificationEventsAreIgnored() {
    UUID idempotencyKey = UUID.fromString("bbbbbbbb-bbbb-cccc-dddd-333333333333");
    String payload = "{\"action\":\"UPD\",\"resource\":\"CUSTOMER_T\"}";

    KafkaTemplate<String, String> producer = createProducer();
    ProducerRecord<String, String> record =
        new ProducerRecord<>("pcis.domain.events", "CUS0000001", payload);
    record
        .headers()
        .add(new RecordHeader("event-type", bytes("AuditEventEnlisted")))
        .add(new RecordHeader("idempotency-key", bytes(idempotencyKey.toString())));

    producer.send(record);
    producer.flush();

    await()
        .pollDelay(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(notificationDispatcher.processedCount()).isZero());
  }

  private KafkaTemplate<String, String> createProducer() {
    var props = KafkaTestUtils.producerProps(embeddedKafka);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
