package com.pcis.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.claims.support.ClaimsTestSecurityConfig;
import com.pcis.claims.support.PostgresTestContainer;
import com.pcis.claims.support.TestEnvironment;
import com.pcis.claims.support.TestJwtFactory;
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
class ClaimsSvcIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private ApplicationContext applicationContext;
  @Autowired private MockMvc mockMvc;

  @Test
  void contextLoadsWithPostgres() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
  }

  @Test
  void claimsSvcApplicationBeanIsPresent() {
    assertThat(applicationContext.containsBean("claimsSvcApplication")).isTrue();
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
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void apiClaimsEndpointReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/claims")).andExpect(status().isUnauthorized());
  }

  @Test
  void apiClaimsEndpointReturns200WithValidJwt() throws Exception {
    mockMvc
        .perform(get("/api/v1/claims").with(TestJwtFactory.asClaimsReader()))
        .andExpect(status().isOk());
  }
}
