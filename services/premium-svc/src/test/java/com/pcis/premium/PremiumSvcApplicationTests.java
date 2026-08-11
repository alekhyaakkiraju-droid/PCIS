package com.pcis.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.premium.support.PostgresTestContainer;
import com.pcis.premium.support.TestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf("com.pcis.premium.support.TestEnvironment#isDockerAvailable")
class PremiumSvcApplicationTests {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void contextLoads() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
  }

  @Test
  void actuatorHealthReturns200() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void unauthenticatedCalculationRequestReturns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/premium/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyType\":\"HOME\",\"state\":\"TX\"}"))
        .andExpect(status().isUnauthorized());
  }
}
