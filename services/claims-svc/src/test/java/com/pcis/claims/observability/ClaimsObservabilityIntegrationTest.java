package com.pcis.claims.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.claims.support.ClaimsTestSecurityConfig;
import com.pcis.claims.support.PostgresTestContainer;
import com.pcis.claims.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "pcis.outbox.relay-enabled=false",
      "spring.task.scheduling.enabled=false",
      "management.endpoint.health.probes.enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClaimsTestSecurityConfig.class)
@EnabledIf("com.pcis.claims.support.TestEnvironment#isDockerAvailable")
class ClaimsObservabilityIntegrationTest {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void prometheusExposesClaimsApiMetrics() throws Exception {
    for (int i = 0; i < 9; i++) {
      mockMvc
          .perform(get("/api/v1/claims").with(TestJwtFactory.asClaimsReader()))
          .andExpect(status().isOk());
    }
    mockMvc.perform(get("/api/v1/claims")).andExpect(status().isUnauthorized());

    String body =
        mockMvc
            .perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).contains("claims_api_request_duration_seconds");
    assertThat(body).contains("claims_api_error_rate");
    assertThat(body).contains("claims_outbox_lag_seconds");
  }
}
