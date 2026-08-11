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
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Proves audit outbox enlistment rolls back atomically with the caller transaction. */
@EnabledIf("dockerAvailable")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = {AuditOutboxTestApplication.class, AuditOutboxTransactionalRollbackIT.TestConfig.class})
@ActiveProfiles("test")
class AuditOutboxTransactionalRollbackIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("pcis_audit_outbox_rollback_test")
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

  @Autowired private OutboxEventRepository repository;
  @Autowired private BusinessMutationService businessMutationService;

  @BeforeEach
  void resetSchema(@Autowired javax.sql.DataSource dataSource) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/audit_outbox.sql"));
    populator.execute(dataSource);
  }

  @Test
  void auditOutboxRowRollsBackWhenTransactionFails() {
    UUID idempotencyKey = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");

    assertThatThrownBy(() -> businessMutationService.mutateAndFail(idempotencyKey))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("simulated business failure");

    assertThat(repository.count()).isZero();
    assertThat(repository.findByIdempotencyKey(idempotencyKey)).isEmpty();
  }

  @Test
  void auditOutboxRowCommitsWhenTransactionSucceeds() {
    UUID idempotencyKey = UUID.fromString("dddddddd-eeee-ffff-0000-111111111111");

    businessMutationService.mutate(idempotencyKey);

    assertThat(repository.count()).isEqualTo(1);
    OutboxEvent saved = repository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(saved.getPayload()).containsEntry("action", "PAY");
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {

    @Bean
    BusinessMutationService businessMutationService(AuditOutboxService auditOutboxService) {
      return new BusinessMutationService(auditOutboxService);
    }
  }

  @Service
  static class BusinessMutationService {

    private final AuditOutboxService auditOutboxService;

    BusinessMutationService(AuditOutboxService auditOutboxService) {
      this.auditOutboxService = auditOutboxService;
    }

    @Transactional
    public void mutateAndFail(UUID idempotencyKey) {
      enlistAudit(idempotencyKey);
      throw new IllegalStateException("simulated business failure");
    }

    @Transactional
    public void mutate(UUID idempotencyKey) {
      enlistAudit(idempotencyKey);
    }

    private void enlistAudit(UUID idempotencyKey) {
      ValidatedAuditEvent validated =
          new ValidatedAuditEvent(
              AuditActionCode.PAY,
              null,
              "1500.00",
              "PAY-42",
              UUID.randomUUID(),
              "claims-svc",
              "CLM006B",
              "BATCHUSER",
              "ClaimPayment",
              "APPROVED_AMT",
              AuditOperation.PAY);
      auditOutboxService.write(new AuditEvent(validated, idempotencyKey));
    }
  }
}
