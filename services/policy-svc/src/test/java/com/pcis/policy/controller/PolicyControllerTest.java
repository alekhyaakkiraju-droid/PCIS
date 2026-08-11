package com.pcis.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.policy.config.SecurityConfig;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.dto.PolicyMapper;
import com.pcis.policy.dto.PolicyResponse;
import com.pcis.policy.exception.GlobalExceptionHandler;
import com.pcis.policy.exception.InvalidStateTransitionException;
import com.pcis.policy.exception.PolicyNotFoundException;
import com.pcis.policy.service.PolicyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, PolicyMapper.class})
class PolicyControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PolicyService policyService;
  @MockBean private JwtDecoder jwtDecoder;

  private PolicyEntity samplePolicy;
  private PolicyResponse sampleResponse;

  @BeforeEach
  void setUp() {
    samplePolicy = new PolicyEntity();
    samplePolicy.setPolNbr("POL10000001");
    samplePolicy.setCustId(1001);
    samplePolicy.setAgtId("AGT00001");
    samplePolicy.setPolicyType("HO-1");
    samplePolicy.setPolStatus("NEW ");
    samplePolicy.setEffDate(LocalDate.of(2027, 1, 1));
    samplePolicy.setExpDate(LocalDate.of(2028, 1, 1));
    samplePolicy.setPremAnnual(new BigDecimal("2400.00"));

    sampleResponse =
        new PolicyResponse(
            "POL10000001",
            1001,
            "AGT00001",
            "HO-1",
            "NEW",
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2028, 1, 1),
            new BigDecimal("2400.00"),
            List.of(),
            null,
            List.of(),
            null,
            null);
  }

  @Test
  void createReturns201ForUnderwriter() throws Exception {
    when(policyService.createPolicy(any())).thenReturn(samplePolicy);

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
                                    .subject("uw-1")
                                    .claim("realm_access", java.util.Map.of("roles", List.of("UNDERWRITER"))))
                        .authorities(new SimpleGrantedAuthority("ROLE_UNDERWRITER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": 1001,
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
        .andExpect(jsonPath("$.policyNumber").value("POL10000001"))
        .andExpect(jsonPath("$.status").value("NEW"));
  }

  @Test
  void createReturns403WithoutUnderwriterRole() throws Exception {
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
                                    .subject("agent-1")
                                    .claim("realm_access", java.util.Map.of("roles", List.of("POLICY_AGENT")))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": 1001,
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
  void getPolicyReturns404WhenMissing() throws Exception {
    when(policyService.findByPolicyNumber("POL99999999"))
        .thenThrow(new PolicyNotFoundException("POL99999999"));

    mockMvc
        .perform(
            get("/api/v1/policies/POL99999999")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder.tokenValue("test").header("alg", "none").subject("reader-1"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.policyNumber").value("POL99999999"));
  }

  @Test
  void cancelReturns409WhenAlreadyCancelled() throws Exception {
    when(policyService.cancelPolicy(eq("POL10000001"), any()))
        .thenThrow(new InvalidStateTransitionException("Policy is already cancelled"));

    mockMvc
        .perform(
            post("/api/v1/policies/POL10000001/cancel")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .tokenValue("test")
                                    .header("alg", "none")
                                    .subject("uw-1")
                                    .claim("realm_access", java.util.Map.of("roles", List.of("UNDERWRITER"))))
                        .authorities(new SimpleGrantedAuthority("ROLE_UNDERWRITER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"cancellationDate": "2027-03-15", "reason": "NONPAY"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void endorseReturns200ForUnderwriter() throws Exception {
    when(policyService.endorsePolicy(eq("POL10000001"), any())).thenReturn(samplePolicy);

    mockMvc
        .perform(
            put("/api/v1/policies/POL10000001/endorse")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .tokenValue("test")
                                    .header("alg", "none")
                                    .subject("uw-1")
                                    .claim("realm_access", java.util.Map.of("roles", List.of("UNDERWRITER"))))
                        .authorities(new SimpleGrantedAuthority("ROLE_UNDERWRITER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "endorsementType": "COV_ADD",
                      "effectiveDate": "2027-06-01",
                      "coverageChanges": [],
                      "reason": "Added flood coverage"
                    }
                    """))
        .andExpect(status().isOk());
  }
}
