package com.pcis.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pcis.observability.logging.PiiMaskingConverter;
import com.pcis.observability.support.ObservabilityTestApplication;
import com.pcis.observability.support.ObservabilityTestApplication.ProbeController;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Primary integration test — MockMvc + in-process Spring Boot (no Docker required).
 *
 * <p>Asserts Prometheus scrape content, correlation header/MDC propagation, and PII masking on the
 * logging path.
 */
@SpringBootTest(
    classes = ObservabilityTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private ListAppender<ILoggingEvent> listAppender;
  private Logger probeLogger;

  @BeforeEach
  void attachLogAppender() {
    probeLogger = (Logger) LoggerFactory.getLogger(ProbeController.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    probeLogger.addAppender(listAppender);
  }

  @AfterEach
  void detachLogAppender() {
    if (probeLogger != null && listAppender != null) {
      probeLogger.detachAppender(listAppender);
    }
  }

  @Test
  void prometheusEndpointExposesJvmAndHttpMetrics() throws Exception {
    mockMvc.perform(get("/api/probe")).andExpect(status().isOk());

    MvcResult prometheus =
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn();

    String body = prometheus.getResponse().getContentAsString();
    assertThat(body).contains("jvm_memory_used_bytes");
    assertThat(body).contains("http_server_requests");
  }

  @Test
  void correlationHeaderAndMdcPropagateAndLogsMaskPii() throws Exception {
    String correlationId = "it-" + UUID.randomUUID();

    MvcResult result =
        mockMvc
            .perform(
                get("/api/probe")
                    .header("X-Correlation-ID", correlationId)
                    .header("X-Program", "AUD001A")
                    .header("X-Actor", "tester"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-ID", correlationId))
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    assertThat(responseBody).contains(correlationId);
    assertThat(responseBody).contains("pcis-observability-it");

    assertThat(listAppender.list).isNotEmpty();
    ILoggingEvent event = listAppender.list.get(listAppender.list.size() - 1);
    assertThat(event.getMDCPropertyMap()).containsEntry("correlationId", correlationId);
    assertThat(event.getMDCPropertyMap()).containsEntry("service", "pcis-observability-it");
    assertThat(event.getMDCPropertyMap()).containsEntry("program", "AUD001A");
    assertThat(event.getMDCPropertyMap()).containsEntry("actor", "tester");

    String masked = PiiMaskingConverter.maskPii(event.getFormattedMessage());
    assertThat(masked).doesNotContain("123-45-6789").doesNotContain("jane.doe@example.com");
    assertThat(masked).contains("6789").contains("example.com");
  }

  @Test
  void healthGroupsAreConfigured() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }
}
