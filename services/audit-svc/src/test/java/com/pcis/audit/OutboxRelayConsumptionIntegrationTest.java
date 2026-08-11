package com.pcis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.pcis.audit.infrastructure.persistence.repository.AuditIngestionIdempotencyRepository;
import com.pcis.audit.infrastructure.persistence.repository.AuditLogRepository;
import com.pcis.audit.support.PostgresTestContainer;
import com.pcis.audit.support.TestSecurityConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EmbeddedKafka(
    partitions = 1,
    topics = {"pcis.domain.events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@TestPropertySource(
    properties = {
      "pcis.audit.kafka.consumer-enabled=true",
      "pcis.outbox.kafka-topic=pcis.domain.events",
      "pcis.outbox.relay-enabled=false",
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
    })
@EnabledIf("com.pcis.audit.support.TestEnvironment#isDockerAvailable")
class OutboxRelayConsumptionIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private AuditIngestionIdempotencyRepository auditIngestionIdempotencyRepository;
  @Autowired private EmbeddedKafkaBroker embeddedKafka;

  @BeforeEach
  void cleanAuditLog() {
    auditLogRepository.deleteAll();
    auditIngestionIdempotencyRepository.deleteAll();
  }

  @Test
  void duplicateRelayedMessagesPersistExactlyOnce() {
    UUID idempotencyKey = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-111111111111");
    String payload =
        """
        {
          "action": "UPD",
          "old_value": "Old Name",
          "new_value": "New Name",
          "key": "CUS0000001",
          "service": "customer-svc",
          "program": "CUS001A",
          "actor": "user001",
          "resource": "CUSTOMER_T",
          "field_name": "CUST_NAME",
          "correlation_id": "22222222-2222-2222-2222-222222222222"
        }
        """;

    KafkaTemplate<String, String> producer = createProducer();
    ProducerRecord<String, String> record =
        new ProducerRecord<>("pcis.domain.events", "CUS0000001", payload);
    record
        .headers()
        .add(new RecordHeader("event-type", bytes("AuditEventEnlisted")))
        .add(new RecordHeader("idempotency-key", bytes(idempotencyKey.toString())));

    producer.send(record);
    producer.send(record);
    producer.flush();

    await()
        .atMost(Duration.ofSeconds(30))
        .pollDelay(Duration.ofMillis(500))
        .untilAsserted(() -> assertThat(auditLogRepository.count()).isEqualTo(1));

    var row = auditLogRepository.findAll().getFirst();
    assertThat(row.getIdempotencyKey()).isEqualTo(idempotencyKey);
    assertThat(row.getKeyValue()).isEqualTo("CUS0000001");
    assertThat(row.getServiceName()).isEqualTo("customer-svc");
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
