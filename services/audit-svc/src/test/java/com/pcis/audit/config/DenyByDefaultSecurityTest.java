package com.pcis.audit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.audit.application.AuditEventService;
import com.pcis.audit.controller.AuditEventController;
import com.pcis.audit.support.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuditEventController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class DenyByDefaultSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuditEventService auditEventService;

  @Test
  void unmappedApiPathRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/v1/audit/unknown")).andExpect(status().isUnauthorized());
  }

  @Test
  void mutatingEndpointRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/v1/audit/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"ADD\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedRequestWithAuditWriteScopeIsNotUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/v1/audit/events")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "action": "ADD",
                      "service": "billing-svc",
                      "actor": "batch",
                      "resource": "BILLING_SCHEDULE_T"
                    }
                    """))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
  }

  private static void assertNotUnauthorized(MvcResult result) {
    assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
  }
}
