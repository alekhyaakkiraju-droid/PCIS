package com.pcis.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.policy.support.PolicyTestSecurityConfig;
import com.pcis.policy.support.PostgresTestContainer;
import com.pcis.policy.support.TestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-context integration test with Testcontainers PostgreSQL 17.
 * Verifies context loads, Flyway migration executes, database connectivity is UP,
 * and the deny-by-default security configuration is correctly applied.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PolicyTestSecurityConfig.class)
@EnabledIf("com.pcis.policy.support.TestEnvironment#isDockerAvailable")
class PolicySvcApplicationTests {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void contextLoadsWithPostgres() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
  }

  @Test
  void policySvcApplicationBeanIsPresent() {
    assertThat(applicationContext.containsBean("policySvcApplication")).isTrue();
  }

  @Test
  void securityFilterChainBeanIsPresent() {
    assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
  }

  @Test
  void correlationIdFilterBeanIsPresent() {
    assertThat(applicationContext.containsBean("correlationIdFilter")).isTrue();
  }

  @Test
  void actuatorHealthReturns200() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  void apiPoliciesEndpointReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/policies"))
        .andExpect(status().isUnauthorized());
  }
}
