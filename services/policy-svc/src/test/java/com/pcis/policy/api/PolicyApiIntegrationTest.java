package com.pcis.policy.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pcis.policy.support.PolicyTestSecurityConfig;
import com.pcis.policy.support.PostgresTestContainer;
import com.pcis.policy.support.TestEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PolicyTestSecurityConfig.class)
@EnabledIf("com.pcis.policy.support.TestEnvironment#isDockerAvailable")
class PolicyApiIntegrationTest {

  private static final WireMockServer AUTHZ_MOCK =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
    AUTHZ_MOCK.start();
    registry.add("pcis.authz-svc.url", AUTHZ_MOCK::baseUrl);
  }

  @AfterAll
  static void stopAuthzMock() {
    AUTHZ_MOCK.stop();
  }

  @BeforeEach
  void seedCoverageTypesAndStubAuthz() {
    jdbcTemplate.update(
        """
        INSERT INTO coverage_type (cov_type, cov_desc, active_flag, crt_user, crt_timestamp)
        VALUES ('HO-1', 'Homeowners Dwelling', 'Y', 'TEST', NOW())
        ON CONFLICT (cov_type) DO NOTHING
        """);
    AUTHZ_MOCK.resetAll();
    AUTHZ_MOCK.stubFor(
        WireMock.post(urlEqualTo("/v1/authz/decisions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"permitted\":true,\"reason\":\"ok\"}")));
  }

  @Test
  void createReadEndorseCancelLifecycle() throws Exception {
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/policies")
                    .with(underwriterJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "customerId": 2001,
                          "agentId": "AGT00001",
                          "policyType": "HO-1",
                          "annualPremium": 2400.00,
                          "effectiveDate": "2027-01-01",
                          "expirationDate": "2028-01-01",
                          "coverages": [{
                            "coverageType": "HO-1",
                            "coverageLimit": 500000.00,
                            "premiumAmount": 2400.00,
                            "deductibles": [{"deductibleType": "STD", "deductibleAmount": 1000.00}]
                          }],
                          "billingPlan": {"billingFrequency": "M", "installmentCount": 12}
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.billingPlan.billingFrequency").value("M"))
            .andReturn();

    String body = createResult.getResponse().getContentAsString();
    String policyNumber =
        body.substring(body.indexOf("\"policyNumber\":\"") + 16);
    policyNumber = policyNumber.substring(0, policyNumber.indexOf('"'));

    mockMvc
        .perform(get("/api/v1/policies/" + policyNumber).with(authenticatedJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coverages[0].deductibles[0].deductibleAmount").value(1000.00));

    mockMvc
        .perform(
            put("/api/v1/policies/" + policyNumber + "/endorse")
                .with(underwriterJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "endorsementType": "COVADD",
                      "effectiveDate": "2027-06-01",
                      "coverageChanges": [{
                        "coverageType": "HO-1",
                        "coverageLimit": 100000.00,
                        "premiumAmount": 100.00,
                        "deductibles": []
                      }],
                      "reason": "Added coverage"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(
            post("/api/v1/policies/" + policyNumber + "/cancel")
                .with(underwriterJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"cancellationDate": "2027-03-15", "reason": "NONPAY"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    Integer outboxCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_events WHERE AGGREGATE_ID = ?",
            Integer.class,
            policyNumber);
    assertThat(outboxCount).isGreaterThanOrEqualTo(3);
  }

  @Test
  void returns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/policies")).andExpect(status().isUnauthorized());
  }

  @Test
  void returns403ForNonUnderwriterOnCreate() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/policies")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .tokenValue("test")
                                    .header("alg", "none")
                                    .subject("csr-1")
                                    .claim("realm_access", java.util.Map.of("roles", java.util.List.of("CSR")))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": 2001,
                      "agentId": "AGT00001",
                      "policyType": "HO-1",
                      "annualPremium": 2400.00,
                      "effectiveDate": "2027-01-01",
                      "expirationDate": "2028-01-01",
                      "coverages": [{
                        "coverageType": "HO-1",
                        "coverageLimit": 500000.00,
                        "premiumAmount": 2400.00,
                        "deductibles": []
                      }],
                      "billingPlan": {"billingFrequency": "M", "installmentCount": 12}
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void returns503WhenAuthzSvcFails() throws Exception {
    AUTHZ_MOCK.resetAll();
    AUTHZ_MOCK.stubFor(
        WireMock.post(urlEqualTo("/v1/authz/decisions")).willReturn(aResponse().withStatus(500)));

    mockMvc
        .perform(
            post("/api/v1/policies")
                .with(underwriterJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": 2001,
                      "agentId": "AGT00001",
                      "policyType": "HO-1",
                      "annualPremium": 2400.00,
                      "effectiveDate": "2027-01-01",
                      "expirationDate": "2028-01-01",
                      "coverages": [{
                        "coverageType": "HO-1",
                        "coverageLimit": 500000.00,
                        "premiumAmount": 2400.00,
                        "deductibles": []
                      }],
                      "billingPlan": {"billingFrequency": "M", "installmentCount": 12}
                    }
                    """))
        .andExpect(status().isServiceUnavailable());
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor underwriterJwt() {
    return jwt()
        .jwt(
            builder ->
                builder
                    .tokenValue("test")
                    .header("alg", "none")
                    .subject("uw-1")
                    .claim("realm_access", java.util.Map.of("roles", java.util.List.of("UNDERWRITER"))))
        .authorities(new SimpleGrantedAuthority("ROLE_UNDERWRITER"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedJwt() {
    return jwt()
        .jwt(
            builder ->
                builder.tokenValue("test").header("alg", "none").subject("reader-1"));
  }
}
