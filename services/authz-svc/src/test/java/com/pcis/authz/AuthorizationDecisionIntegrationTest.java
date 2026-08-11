package com.pcis.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.authz.application.AuthorizationDecisionOutboxWriter;
import com.pcis.authz.application.PolicyDecisionService;
import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.support.PostgresTestContainer;
import com.pcis.authz.support.TestEnvironment;
import com.pcis.authz.support.TestSecurityConfig;
import com.pcis.outbox.OutboxEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.authz.support.TestEnvironment#isDockerAvailable")
class AuthorizationDecisionIntegrationTest {

  private static final UUID CORRELATION_ID =
      UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private OutboxEventRepository outboxEventRepository;

  @BeforeEach
  void cleanOutbox() {
    outboxEventRepository.deleteAll();
  }

  @Test
  void permitDecisionForGrantedPermission() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("adjuster-001")))
                .header("X-Correlation-Id", CORRELATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"claim","operation":"read"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("PERMIT"))
        .andExpect(jsonPath("$.reasonCode").value("GRANT_MATCH"))
        .andExpect(jsonPath("$.evaluatedPermissions").isArray())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID.toString()));

    assertThat(outboxEventRepository.count()).isEqualTo(1);
    var event = outboxEventRepository.findAll().getFirst();
    assertThat(event.getEventType())
        .isEqualTo(AuthorizationDecisionOutboxWriter.EVENT_TYPE);
    assertThat(event.getPayload()).containsEntry("decision", "PERMIT");
  }

  @Test
  void denyByDefaultForUnmappedResourceOperation() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("adjuster-001")))
                .header("X-Correlation-Id", CORRELATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"customer","operation":"write"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("DENY"))
        .andExpect(jsonPath("$.reasonCode").value("NO_GRANT"));
  }

  @Test
  void unassignedPrincipalDeniedForAllSeededPairs() throws Exception {
    for (AuthorizationRequest request : PolicyDecisionService.allSeededResourceOperations()) {
      outboxEventRepository.deleteAll();
      mockMvc
          .perform(
              post("/v1/authz/decisions")
                  .with(jwt().jwt(token -> token.subject("unknown-user")))
                  .header("X-Correlation-Id", CORRELATION_ID.toString())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"resource":"%s","operation":"%s"}
                      """
                          .formatted(request.resource(), request.operation())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.decision").value("DENY"))
          .andExpect(jsonPath("$.reasonCode").value("NO_GRANT"));
    }
  }

  @Test
  void singleGrantFlipsOnlyMatchingPair() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("csr-001")))
                .header("X-Correlation-Id", CORRELATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"customer","operation":"write"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("PERMIT"));

    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("csr-001")))
                .header("X-Correlation-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"claim","operation":"read"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("DENY"))
        .andExpect(jsonPath("$.reasonCode").value("NO_GRANT"));
  }

  @Test
  void missingBearerTokenReturns401ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"claim","operation":"read"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.title").value("Unauthorized"));
  }

  @Test
  void invalidTokenReturns401ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"claim","operation":"read"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void initiatePaymentRoutesToPaymentAuthorityCheck() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("adjuster-001")))
                .header("X-Correlation-Id", CORRELATION_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "resource":"claim",
                      "operation":"INITIATE_PAYMENT",
                      "context":{
                        "claimId":"CLM0001001",
                        "reserveId":1001,
                        "requestedAmount":10000.00
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("DENY"))
        .andExpect(jsonPath("$.reasonCode").value("APPROVAL_MISSING"));
  }

  @Test
  void validationFailureReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("adjuster-001")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource":"","operation":"read"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Authorization request validation failed"));
  }
}
