package com.pcis.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.audit.contract.AuditActionCode;
import com.pcis.audit.contract.AuditOperation;
import com.pcis.audit.contract.ValidatedAuditEvent;
import com.pcis.audit.outbox.support.AuditOutboxTestApplication;
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
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Proves {@link AuditOutboxService#write} requires an active caller transaction. */
@EnabledIf("dockerAvailable")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AuditOutboxTestApplication.class)
@ActiveProfiles("test")
class AuditOutboxMandatoryTransactionIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("pcis_audit_outbox_test")
          .withUsername("pcis")
          .withPassword("pcis");

  @DynamicPropertySource
  static void registerDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  static boolean dockerAvailable() {
    try {
      return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  @Autowired private AuditOutboxService auditOutboxService;
  @Autowired private OutboxEventRepository repository;

  @BeforeEach
  void resetSchema(@Autowired javax.sql.DataSource dataSource) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/audit_outbox.sql"));
    populator.execute(dataSource);
  }

  @Test
  void writeFailsFastWhenNoActiveTransaction() {
    AuditEvent event = sampleEvent(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));

    assertThatThrownBy(() -> auditOutboxService.write(event))
        .isInstanceOf(IllegalTransactionStateException.class)
        .hasMessageContaining("No existing transaction found");

    assertThat(repository.count()).isZero();
  }

  @Test
  @Transactional
  void writeSucceedsInsideActiveTransaction() {
    UUID idempotencyKey = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");
    AuditEvent event = sampleEvent(idempotencyKey);

    auditOutboxService.write(event);

    assertThat(repository.count()).isEqualTo(1);
    OutboxEvent saved = repository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(saved.getPayload()).containsEntry("action", "UPD");
    assertThat(saved.getPayload()).containsEntry("service", "billing-svc");
  }

  private static AuditEvent sampleEvent(UUID idempotencyKey) {
    ValidatedAuditEvent validated =
        new ValidatedAuditEvent(
            AuditActionCode.UPD,
            "due",
            "paid",
            "INST-100",
            UUID.randomUUID(),
            "billing-svc",
            "BIL003B",
            "BATCHUSER",
            "Installment",
            "STATUS",
            AuditOperation.UPDATE);
    return new AuditEvent(validated, idempotencyKey);
  }
}
