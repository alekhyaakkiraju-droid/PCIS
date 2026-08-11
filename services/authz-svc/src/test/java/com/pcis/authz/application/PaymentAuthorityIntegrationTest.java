package com.pcis.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.authz.application.AuthorizationDecisionOutboxWriter;
import com.pcis.authz.domain.decision.ReasonCode;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.authz.support.TestEnvironment#isDockerAvailable")
@Sql(
    scripts = "classpath:db/testdata/payment_authority_baseline.sql",
    config = @SqlConfig(separator = ";"))
class PaymentAuthorityIntegrationTest {

  private static final UUID CORRELATION_ID =
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private PaymentAuthorityService paymentAuthorityService;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanOutbox() {
    outboxEventRepository.deleteAll();
  }

  @Test
  void checkPaymentAuthorityDeniesWhenApprovalMissing() {
    jdbcTemplate.update("DELETE FROM APPROVAL_T WHERE RESERVE_HIST_ID = 1001");

    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001001", 1001L, new java.math.BigDecimal("1000.00"), "ADJ1000001");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.APPROVAL_MISSING);
  }

  @Test
  void checkPaymentAuthorityDeniesPb01CumulativeLimitExceeded() {
    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001001", 1001L, new java.math.BigDecimal("10000.00"), "ADJ1000001");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.AUTHORITY_LIMIT_EXCEEDED);
  }

  @Test
  void checkPaymentAuthorityPermitsWithinCumulativeLimit() {
    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001001", 1001L, new java.math.BigDecimal("5000.00"), "ADJ1000001");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_GRANTED);
    assertThat(result.approvalId()).isEqualTo(2001L);
    assertThat(result.approverPrincipal()).isEqualTo("ADJ1000002");
  }

  @Test
  @Sql(
      scripts = {
        "classpath:db/testdata/payment_authority_baseline.sql",
        "classpath:db/testdata/sod-scenarios.sql"
      },
      config = @SqlConfig(separator = ";"))
  void sodDeniesWhenApproverEqualsDisburser() {
    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001201", 1201L, new java.math.BigDecimal("1000.00"), "BOB");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.SELF_APPROVAL_FORBIDDEN);
    assertThat(result.maskedApproverPrincipal()).isEqualTo("BOB");
    assertThat(result.maskedDisburserPrincipal()).isEqualTo("BOB");
  }

  @Test
  @Sql(
      scripts = {
        "classpath:db/testdata/payment_authority_baseline.sql",
        "classpath:db/testdata/sod-scenarios.sql"
      },
      config = @SqlConfig(separator = ";"))
  void sodPermitsWhenDisburserDiffersFromApprover() {
    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001201", 1201L, new java.math.BigDecimal("1000.00"), "ALICE");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_GRANTED);
  }

  @Test
  void initiatePaymentDecisionWritesOutboxInSameTransaction() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("ADJ1000001")))
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
                        "requestedAmount":5000.00,
                        "adjusterId":"ADJ1000001"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("PERMIT"))
        .andExpect(jsonPath("$.reasonCode").value("PAYMENT_AUTHORITY_GRANTED"));

    assertThat(outboxEventRepository.count()).isEqualTo(1);
    var event = outboxEventRepository.findAll().getFirst();
    assertThat(event.getEventType()).isEqualTo(AuthorizationDecisionOutboxWriter.EVENT_TYPE);
    assertThat(event.getPayload()).containsEntry("reasonCode", "PAYMENT_AUTHORITY_GRANTED");
  }

  @Test
  @Sql(
      scripts = {
        "classpath:db/testdata/payment_authority_baseline.sql",
        "classpath:db/testdata/payment_authority_boundary.sql"
      },
      config = @SqlConfig(separator = ";"))
  void boundaryPermitsOneCentUnderLimit() {
    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001002", 1101L, new java.math.BigDecimal("999.99"), "ADJ1000001");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.PAYMENT_AUTHORITY_GRANTED);
  }

  @Test
  @Sql(
      scripts = {
        "classpath:db/testdata/payment_authority_baseline.sql",
        "classpath:db/testdata/payment_authority_no_approval.sql"
      },
      config = @SqlConfig(separator = ";"))
  void noApprovalFixtureDenies() {
    var reserves =
        jdbcTemplate.queryForList(
            "SELECT RESERVE_HIST_ID FROM CLAIM_RESERVE_T WHERE CLAIM_ID = 'CLM0001003'");
    Long reserveId = ((Number) reserves.getFirst().get("RESERVE_HIST_ID")).longValue();

    var result =
        paymentAuthorityService.checkPaymentAuthority(
            "CLM0001003", reserveId, new java.math.BigDecimal("1000.00"), "ADJ1000001");

    assertThat(result.reasonCode()).isEqualTo(ReasonCode.APPROVAL_MISSING);
  }
}
