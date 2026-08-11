package com.pcis.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.billing.support.BillingTestSecurityConfig;
import com.pcis.billing.support.PostgresTestContainer;
import com.pcis.billing.support.TestEnvironment;
import com.pcis.billing.support.TestJwtFactory;
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
      "management.endpoint.health.probes.enabled=false",
      "management.endpoint.health.group.liveness.include=ping",
      "management.endpoint.health.group.readiness.include=ping,db",
      "management.endpoint.health.group.startup.include=ping,db"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BillingTestSecurityConfig.class)
@EnabledIf("com.pcis.billing.support.TestEnvironment#isDockerAvailable")
class BillingSvcSmokeTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private ApplicationContext applicationContext;
  @Autowired private MockMvc mockMvc;

  @Test
  void contextLoadsWithPostgres() {
    assertThat(TestEnvironment.isDockerAvailable()).isTrue();
    assertThat(applicationContext.containsBean("billingSvcApplication")).isTrue();
  }

  @Test
  void actuatorHealthReturns200() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void createScheduleReturns401WithoutJwt() throws Exception {
    mockMvc.perform(post("/v1/billing/schedules")).andExpect(status().isUnauthorized());
  }

  @Test
  void createScheduleReturns501WithWriteScope() throws Exception {
    mockMvc
        .perform(post("/v1/billing/schedules").with(TestJwtFactory.asBillingWriter()))
        .andExpect(status().isNotImplemented());
  }
}
