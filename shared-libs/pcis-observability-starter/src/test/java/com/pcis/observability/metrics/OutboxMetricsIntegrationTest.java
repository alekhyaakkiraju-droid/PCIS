package com.pcis.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.observability.support.ObservabilityTestApplication;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for outbox relay metrics against PostgreSQL (WO-143).
 *
 * <p>Disabled when Docker is unavailable (JUnit reports skipped).
 */
@EnabledIf("dockerAvailable")
@Testcontainers
@SpringBootTest(
    classes = {ObservabilityTestApplication.class, OutboxMetricsIntegrationTest.OutboxTestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("outbox-it")
class OutboxMetricsIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("pcis")
          .withUsername("pcis")
          .withPassword("pcis");

  @Autowired private MockMvc mockMvc;
  @Autowired private OutboxMetrics outboxMetrics;

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  @DynamicPropertySource
  static void registerDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void loadFixtures(@Autowired javax.sql.DataSource dataSource) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("fixtures/outbox_events.sql"));
    populator.setSeparator(";");
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.execute(dataSource);
    outboxMetrics.refreshMetrics();
  }

  @Test
  void prometheusExposesOutboxMetricsFromDatabase() throws Exception {
    assertThat(outboxMetrics.pendingCountValue()).isEqualTo(3.0);
    assertThat(outboxMetrics.lagSecondsValue()).isBetween(55.0, 65.0);

    MvcResult result =
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("pcis_audit_outbox_pending_count");
    assertThat(body).contains("pcis_audit_outbox_lag_seconds");
    assertThat(body).contains("service=\"audit-svc-test\"");
  }

  @TestConfiguration
  static class OutboxTestConfig {

    @Bean
    OutboxEventMetricsRepository outboxEventMetricsRepository(javax.sql.DataSource dataSource) {
      return new JdbcOutboxEventMetricsRepository(dataSource);
    }
  }
}
