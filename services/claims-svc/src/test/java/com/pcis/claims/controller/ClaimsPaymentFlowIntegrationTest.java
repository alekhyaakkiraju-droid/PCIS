package com.pcis.claims.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.claims.application.ClaimsApplicationService;
import com.pcis.claims.dto.CreateApprovalRequest;
import com.pcis.claims.dto.CreateClaimRequest;
import com.pcis.claims.dto.CreatePaymentRequest;
import com.pcis.claims.dto.CreateReserveRequest;
import com.pcis.claims.dto.UpdateClaimRequest;
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
class ClaimsPaymentFlowIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ClaimsApplicationService claimsApplicationService;

  private static String uniqueClaimNbr() {
    return "CLM" + String.format("%09d", Math.abs((int) (System.nanoTime() % 1_000_000_000L)));
  }

  @Test
  void fullPaymentFlow_succeeds() throws Exception {
    String claimNbr = uniqueClaimNbr();
    claimsApplicationService.ensureAdjuster("ADJ001", new BigDecimal("10000.00"));

    CreateClaimRequest claimRequest =
        new CreateClaimRequest(
            claimNbr, "POL000000001", 1001, LocalDate.of(2026, 4, 10), "PRP", null);
    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsAdjuster("ADJ001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(claimRequest)))
        .andExpect(status().isCreated());

    CreateReserveRequest reserveRequest =
        new CreateReserveRequest("PRO", new BigDecimal("8000.00"));
    MvcResult reserveResult =
        mockMvc
            .perform(
                post("/api/v1/claims/" + claimNbr + "/reserves")
                    .with(TestJwtFactory.asClaimsAdjuster("ADJ001"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reserveRequest)))
            .andExpect(status().isCreated())
            .andReturn();
    Long reserveId =
        objectMapper.readTree(reserveResult.getResponse().getContentAsString()).get("reserveId").asLong();

    CreateApprovalRequest approvalRequest = new CreateApprovalRequest(reserveId);
    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/approvals")
                .with(TestJwtFactory.asClaimsSupervisor("SUP001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.approvalStatus").value("A"));

    CreatePaymentRequest paymentRequest =
        new CreatePaymentRequest(reserveId, new BigDecimal("2500.00"), 1001);
    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/payments")
                .with(TestJwtFactory.asClaimsAdjuster("ADJ001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentAmt").value(2500.00))
        .andExpect(jsonPath("$.approvalId").exists());
  }

  @Test
  void payment_rejectsSegregationOfDuties() throws Exception {
    String claimNbr = uniqueClaimNbr();
    claimsApplicationService.ensureAdjuster("ADJ001", new BigDecimal("10000.00"));

    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsAdjuster("ADJ001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateClaimRequest(
                            claimNbr, "POL000000001", 1001, LocalDate.of(2026, 4, 11), "PRP", null))))
        .andExpect(status().isCreated());

    MvcResult reserveResult =
        mockMvc
            .perform(
                post("/api/v1/claims/" + claimNbr + "/reserves")
                    .with(TestJwtFactory.asClaimsAdjuster("ADJ001"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateReserveRequest("PRO", new BigDecimal("5000.00")))))
            .andExpect(status().isCreated())
            .andReturn();
    Long reserveId =
        objectMapper.readTree(reserveResult.getResponse().getContentAsString()).get("reserveId").asLong();

    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/approvals")
                .with(TestJwtFactory.asClaimsSupervisor("ADJ001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApprovalRequest(reserveId))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/payments")
                .with(TestJwtFactory.asClaimsAdjuster("ADJ001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreatePaymentRequest(reserveId, new BigDecimal("100.00"), 1001))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PAYMENT_AUTHORIZATION_DENIED"));
  }

  @Test
  void payment_rejectsAuthorityLimit() throws Exception {
    String claimNbr = uniqueClaimNbr();
    String adjusterId = "ADJ" + String.format("%07d", Math.abs((int) (System.nanoTime() % 10_000_000L)));
    claimsApplicationService.ensureAdjuster(adjusterId, new BigDecimal("1000.00"));

    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsAdjuster(adjusterId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateClaimRequest(
                            claimNbr, "POL000000001", 1001, LocalDate.of(2026, 4, 12), "PRP", null))))
        .andExpect(status().isCreated());

    MvcResult reserveResult =
        mockMvc
            .perform(
                post("/api/v1/claims/" + claimNbr + "/reserves")
                    .with(TestJwtFactory.asClaimsAdjuster(adjusterId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateReserveRequest("PRO", new BigDecimal("5000.00")))))
            .andExpect(status().isCreated())
            .andReturn();
    Long reserveId =
        objectMapper.readTree(reserveResult.getResponse().getContentAsString()).get("reserveId").asLong();

    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/approvals")
                .with(TestJwtFactory.asClaimsSupervisor("SUP001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApprovalRequest(reserveId))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/claims/" + claimNbr + "/payments")
                .with(TestJwtFactory.asClaimsAdjuster(adjusterId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreatePaymentRequest(reserveId, new BigDecimal("1500.00"), 1001))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.authorityLimit").value(1000.00))
        .andExpect(jsonPath("$.requestedAmount").value(1500.00));
  }

  @Test
  void updateClaim_returns409OnVersionConflict() throws Exception {
    String claimNbr = uniqueClaimNbr();
    mockMvc
        .perform(
            post("/api/v1/claims")
                .with(TestJwtFactory.asClaimsWriter())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateClaimRequest(
                            claimNbr, "POL000000001", 1001, LocalDate.of(2026, 4, 13), "PRP", null))))
        .andExpect(status().isCreated());

    MvcResult getResult =
        mockMvc
            .perform(get("/api/v1/claims/" + claimNbr).with(TestJwtFactory.asClaimsReader()))
            .andExpect(status().isOk())
            .andExpect(header().exists("ETag"))
            .andReturn();

    String etag = getResult.getResponse().getHeader("ETag");
    UpdateClaimRequest update = new UpdateClaimRequest("C", null, null);

    mockMvc
        .perform(
            put("/api/v1/claims/" + claimNbr)
                .with(TestJwtFactory.asClaimsWriter())
                .header("If-Match", etag)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.claimStatus").value("C"));

    mockMvc
        .perform(
            put("/api/v1/claims/" + claimNbr)
                .with(TestJwtFactory.asClaimsWriter())
                .header("If-Match", etag)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isConflict());
  }
}
