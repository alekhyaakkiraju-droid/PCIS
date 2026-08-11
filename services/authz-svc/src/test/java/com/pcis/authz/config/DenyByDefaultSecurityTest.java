package com.pcis.authz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest
@Import(SecurityConfig.class)
class DenyByDefaultSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void unmappedApiPathReturns403() throws Exception {
    mockMvc.perform(get("/api/v1/authz/decisions")).andExpect(status().isForbidden());
  }

  @Test
  void rootPathReturns403() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isForbidden());
  }

  @Test
  void actuatorHealthIsNotDenied() throws Exception {
    assertNotDenied(mockMvc.perform(get("/actuator/health")).andReturn());
  }

  @Test
  void actuatorReadinessIsNotDenied() throws Exception {
    assertNotDenied(mockMvc.perform(get("/actuator/readiness")).andReturn());
  }

  @Test
  void actuatorPrometheusIsNotDenied() throws Exception {
    assertNotDenied(mockMvc.perform(get("/actuator/prometheus")).andReturn());
  }

  private static void assertNotDenied(MvcResult result) {
    assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
  }
}
