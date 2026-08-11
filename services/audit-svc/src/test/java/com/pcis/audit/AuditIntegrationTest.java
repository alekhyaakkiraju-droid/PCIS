package com.pcis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.audit.infrastructure.persistence.repository.AuditLogRepository;
import com.pcis.audit.support.FixtureLoader;
import com.pcis.audit.support.PostgresTestContainer;
import com.pcis.audit.support.TestEnvironment;
import com.pcis.audit.support.TestSecurityConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
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
@EnabledIf("com.pcis.audit.support.TestEnvironment#isDockerAvailable")
class AuditIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void postAuditEventPersistsToPartition() throws Exception {
    Map<String, Object> payload = FixtureLoader.loadAuditEvents().get("interactive_cus001a_update");

    mockMvc
        .perform(
            post("/v1/audit/events")
                .header("Authorization", "Bearer integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.audit_log_id").isNumber())
        .andExpect(jsonPath("$.operation").value("UPDATE"))
        .andExpect(jsonPath("$.correlation_id").value("44444444-4444-4444-4444-444444444444"));

    assertThat(auditLogRepository.count()).isEqualTo(1);
  }

  @Test
  void fixturesCoverBatchAndInteractiveLegacyShapes() throws Exception {
    var fixtures = FixtureLoader.loadAuditEvents();
    assertThat(fixtures).containsKeys(
        "batch_bil003b_init",
        "batch_clm006b_pay",
        "interactive_cus001a_update",
        "interactive_pol001a_insert");

    for (Map.Entry<String, Map<String, Object>> entry : fixtures.entrySet()) {
      auditLogRepository.deleteAll();
      mockMvc
          .perform(
              post("/v1/audit/events")
                  .header("Authorization", "Bearer integration-test")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(entry.getValue())))
          .andExpect(status().isCreated());
    }
  }

  @Test
  void unknownActionReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/v1/audit/events")
                .header("Authorization", "Bearer integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "action": "ZZZ",
                      "service": "billing-svc",
                      "actor": "batch",
                      "resource": "BILLING_SCHEDULE_T"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Audit event validation failed"));
  }

  @Test
  void applicationRoleCannotUpdateAuditLog() throws Exception {
    Map<String, Object> payload = FixtureLoader.loadAuditEvents().get("batch_pol006b_renew");
    mockMvc
        .perform(
            post("/v1/audit/events")
                .header("Authorization", "Bearer integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated());

    try (Connection conn =
        PostgresTestContainer.container().getJdbcUrl().startsWith("jdbc")
            ? java.sql.DriverManager.getConnection(
                PostgresTestContainer.container().getJdbcUrl(),
                "pcis_audit_app",
                "pcis_audit_app")
            : null) {
      assertThat(conn).isNotNull();
      var ex =
          org.junit.jupiter.api.Assertions.assertThrows(
              SQLException.class,
              () ->
                  conn.createStatement()
                      .executeUpdate("UPDATE audit_log SET actor = 'hacker' WHERE actor = 'batchren'"));
      assertThat(ex.getMessage()).containsIgnoringCase("permission denied");
    }
  }

  @Test
  void widestFieldMappingPreservesHundredCharacterValue() throws Exception {
    String hundred = "X".repeat(100);
    mockMvc
        .perform(
            post("/v1/audit/events")
                .header("Authorization", "Bearer integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "action": "C",
                      "old_value": "%s",
                      "new_value": "%s",
                      "key": "%s",
                      "service": "customer-svc",
                      "program": "CUS001A",
                      "actor": "user001",
                      "resource": "CUSTOMER_T",
                      "field_name": "NOTES"
                    }
                    """
                        .formatted(hundred, hundred, "K".repeat(40))))
        .andExpect(status().isCreated());

    var row = auditLogRepository.findAll().getFirst();
    assertThat(row.getOldValue()).hasSize(100);
    assertThat(row.getKeyValue()).hasSize(40);
  }
}
