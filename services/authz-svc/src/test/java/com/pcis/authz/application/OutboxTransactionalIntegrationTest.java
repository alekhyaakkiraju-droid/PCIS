package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.authz.support.PostgresTestContainer;
import com.pcis.authz.support.TestEnvironment;
import com.pcis.authz.support.TestSecurityConfig;
import com.pcis.outbox.OutboxEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, OutboxTransactionalIntegrationTest.TestConfig.class})
@EnabledIf("com.pcis.authz.support.TestEnvironment#isDockerAvailable")
class OutboxTransactionalIntegrationTest {

  private static final UUID CORRELATION_ID =
      UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private FailingDecisionService failingDecisionService;

  @BeforeEach
  void cleanOutbox() {
    outboxEventRepository.deleteAll();
  }

  @Test
  void outboxRowRollsBackWhenTransactionFails() {
    assertThatThrownBy(
            () ->
                failingDecisionService.decideAndFail(
                    "adjuster-001", "claim", "read", CORRELATION_ID.toString()))
        .isInstanceOf(IllegalStateException.class);

    assertThat(outboxEventRepository.count()).isZero();
  }

  @Service
  static class FailingDecisionService {

    private final AuthorizationDecisionService authorizationDecisionService;

    FailingDecisionService(AuthorizationDecisionService authorizationDecisionService) {
      this.authorizationDecisionService = authorizationDecisionService;
    }

    @Transactional
    public void decideAndFail(
        String principalId, String resource, String operation, String correlationId) {
      authorizationDecisionService.decide(
          principalId,
          new com.pcis.authz.contract.AuthorizationRequest(resource, operation, null),
          correlationId);
      throw new IllegalStateException("simulated failure");
    }
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {
    @Bean
    FailingDecisionService failingDecisionService(AuthorizationDecisionService service) {
      return new FailingDecisionService(service);
    }
  }
}
