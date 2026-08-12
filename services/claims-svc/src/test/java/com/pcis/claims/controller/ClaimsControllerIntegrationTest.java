package com.pcis.claims.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import com.pcis.claims.support.ClaimsTestSecurityConfig;
import com.pcis.claims.support.PostgresTestContainer;
import com.pcis.claims.support.TestEnvironment;
import com.pcis.claims.support.TestJwtFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.main.allow-bean-definition-overriding=true",
      "management.endpoint.health.probes.enabled=false",
      "management.endpoint.health.group.liveness.include=ping",
      "management.endpoint.health.group.readiness.include=ping,db",
      "management.endpoint.health.group.startup.include=ping,db"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClaimsTestSecurityConfig.class)
@EnabledIf("com.pcis.claims.support.TestEnvironment#isDockerAvailable")
class ClaimsControllerIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private static String uniqueClaimNbr() {
    return "CLM" + String.format("%09d", Math.abs((int) (System.nanoTime() % 1_000_000_000L)));
  }

  @Test
  void createAndReadClaim() throws Exception {
    String claimNbr = uniqueClaimNbr();
    CreateClaimRequest request =
        new CreateClaimRequest(
            claimNbr, "POL000000001", 1001, LocalDate.of(2026, 4, 1), "PRP", null, null, null);

    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.claimNbr").value(claimNbr))
        .andExpect(jsonPath("$.custId").value(1001))
        .andExpect(jsonPath("$.claimStatus").value("O"));

    mockMvc
        .perform(
            get("/api/v1/claims/" + claimNbr).with(TestJwtFactory.asClaimsReader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.polNbr").value("POL000000001"));
  }

  @Test
  void createReserveAndListReserves() throws Exception {
    String claimNbr = uniqueClaimNbr();
    CreateClaimRequest claimRequest =
        new CreateClaimRequest(
            claimNbr, "POL000000001", 1001, LocalDate.of(2026, 4, 2), "PRP", null, null, null);
    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(claimRequest)))
        .andExpect(status().isCreated());

    CreateReserveRequest reserveRequest =
        new CreateReserveRequest("PRO", new BigDecimal("5000.00"), null);
    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/reserves")
                .with(TestJwtFactory.asClaimsWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reserveRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reserveType").value("PRO"))
        .andExpect(jsonPath("$.approvedAmt").value(5000.00));

    MvcResult listResult =
        mockMvc
            .perform(
                get("/api/v1/claims/" + claimNbr + "/reserves")
                    .with(TestJwtFactory.asClaimsReader()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(listResult.getResponse().getContentAsString()).contains("PRO");
  }

  @Test
  void listClaimsByCustomerReturnsMatchingClaims() throws Exception {
    String claimNbr = uniqueClaimNbr();
    CreateClaimRequest request =
        new CreateClaimRequest(
            claimNbr, "POL000000001", 4242, LocalDate.of(2026, 4, 3), "PRP", null, null, null);
    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/v1/claims/customer/4242").with(TestJwtFactory.asClaimsReader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].claimNbr").value(claimNbr))
        .andExpect(jsonPath("$[0].custId").value(4242));
  }

  @Test
  void getClaimReturnsNotFoundProblemDetail() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/claims/CLM999999999").with(TestJwtFactory.asClaimsReader()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SYS_NOT_FOUND"));
  }

  @Test
  void writeEndpointReturnsForbiddenWithoutWriteScope() throws Exception {
    CreateClaimRequest request =
        new CreateClaimRequest(
            uniqueClaimNbr(), "POL000000001", 1001, LocalDate.of(2026, 4, 4), "PRP", null, null, null);
    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsReader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
