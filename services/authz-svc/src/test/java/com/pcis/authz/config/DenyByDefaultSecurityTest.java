package com.pcis.authz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.authz.api.AuthorizationDecisionController;
import com.pcis.authz.application.AuthorizationDecisionService;
import com.pcis.authz.support.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthorizationDecisionController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class DenyByDefaultSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthorizationDecisionService authorizationDecisionService;

  @Test
  void unmappedApiPathRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/authz/decisions")).andExpect(status().isUnauthorized());
  }

  @Test
  void decisionsEndpointRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resource\":\"claim\",\"operation\":\"read\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedDecisionRequestIsNotUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/v1/authz/decisions")
                .with(jwt().jwt(token -> token.subject("adjuster-001")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resource\":\"claim\",\"operation\":\"read\"}"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
  }
}
