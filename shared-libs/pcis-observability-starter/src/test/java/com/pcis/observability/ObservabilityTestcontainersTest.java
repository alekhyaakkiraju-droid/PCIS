package com.pcis.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.observability.support.ObservabilityTestApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;

/**
 * Optional Testcontainers-gated smoke path.
 *
 * <p>Disabled via {@link EnabledIf} when Docker is unavailable (reported as skipped). Primary
 * coverage is {@link ObservabilityIntegrationTest} (MockMvc, no Docker).
 */
@EnabledIf("dockerAvailable")
@SpringBootTest(
    classes = ObservabilityTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityTestcontainersTest {

  @Autowired private MockMvc mockMvc;

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  @Test
  void prometheusAndProbeWorkWhenDockerAvailable() throws Exception {
    mockMvc.perform(get("/api/probe")).andExpect(status().isOk());
    MvcResult prometheus =
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn();
    assertThat(prometheus.getResponse().getContentAsString()).contains("jvm_memory_used_bytes");
  }
}
