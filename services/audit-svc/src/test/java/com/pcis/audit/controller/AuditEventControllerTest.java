package com.pcis.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.audit.application.AuditEventService;
import com.pcis.audit.config.SecurityConfig;
import com.pcis.audit.contract.AuditEventResponse;
import com.pcis.audit.support.TestSecurityConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditEventController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class AuditEventControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuditEventService auditEventService;

  @Test
  void recordEventReturnsCreatedResponse() throws Exception {
    when(auditEventService.recordEvent(any()))
        .thenReturn(
            new AuditEventResponse(
                7L,
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "CREATE",
                Instant.parse("2026-08-11T12:00:00Z")));

    mockMvc
        .perform(
            post("/v1/audit/events")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "action": "A",
                      "service": "policy-svc",
                      "actor": "user001",
                      "resource": "POLICY_T"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.audit_log_id").value(7))
        .andExpect(jsonPath("$.operation").value("CREATE"));
  }
}
