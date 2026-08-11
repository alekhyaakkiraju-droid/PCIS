package com.pcis.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.outbox.support.OutboxTestApplication;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves outbox enlistment rolls back atomically with the caller transaction.
 */
@EnabledIf("dockerAvailable")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = {OutboxTestApplication.class, OutboxTransactionalRollbackTest.TestConfig.class})
@ActiveProfiles("test")
class OutboxTransactionalRollbackTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("pcis_outbox_test")
          .withUsername("pcis")
          .withPassword("pcis");

  @DynamicPropertySource
  static void registerDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("pcis.outbox.relay-enabled", () -> "false");
  }

  static boolean dockerAvailable() {
    try {
      return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  @Autowired private OutboxEventRepository repository;
  @Autowired private TransactionalOutboxWriter transactionalOutboxWriter;

  @BeforeEach
  void resetSchema(@Autowired javax.sql.DataSource dataSource) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/outbox_events.sql"));
    populator.execute(dataSource);
  }

  @Test
  void outboxRowRollsBackWhenTransactionFails() {
    UUID idempotencyKey = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    assertThatThrownBy(
            () ->
                transactionalOutboxWriter.enlistAndFail(
                    "Policy", "POL-999", "PolicyIssued", Map.of("premium", 1200), idempotencyKey))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("simulated business failure");

    assertThat(repository.count()).isZero();
    assertThat(repository.findByIdempotencyKey(idempotencyKey)).isEmpty();
  }

  @Test
  void outboxRowCommitsWhenTransactionSucceeds() {
    UUID idempotencyKey = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    transactionalOutboxWriter.enlist(
        "Policy", "POL-100", "PolicyIssued", Map.of("premium", 900), idempotencyKey);

    assertThat(repository.count()).isEqualTo(1);
    OutboxEvent saved = repository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(saved.isPublished()).isFalse();
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {

    @org.springframework.context.annotation.Bean
    TransactionalOutboxWriter transactionalOutboxWriter(OutboxEventRepository repository) {
      return new TransactionalOutboxWriter(repository);
    }
  }

  static class TransactionalOutboxWriter {

    private final OutboxEventRepository repository;

    TransactionalOutboxWriter(OutboxEventRepository repository) {
      this.repository = repository;
    }

    @Transactional
    public void enlistAndFail(
        String aggregateType,
        String aggregateId,
        String eventType,
        Map<String, Object> payload,
        UUID idempotencyKey) {
      enlist(aggregateType, aggregateId, eventType, payload, idempotencyKey);
      throw new IllegalStateException("simulated business failure");
    }

    @Transactional
    public void enlist(
        String aggregateType,
        String aggregateId,
        String eventType,
        Map<String, Object> payload,
        UUID idempotencyKey) {
      OutboxEvent event = new OutboxEvent();
      event.setAggregateType(aggregateType);
      event.setAggregateId(aggregateId);
      event.setEventType(eventType);
      event.setPayload(payload);
      event.setIdempotencyKey(idempotencyKey);
      event.setStatus(OutboxEventStatus.PENDING);
      event.setAttemptCount(0);
      event.setCreatedAt(Instant.now());
      event.setNextAttemptAt(Instant.now());
      event.setCreatedBy("TESTUSER");
      repository.save(event);
    }
  }
}
