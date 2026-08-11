package com.pcis.claims.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.observability.metrics.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ClaimsOutboxMetricsConfigTest {

  @Test
  void registersClaimsMetricNames() {
    MeterRegistry registry = new SimpleMeterRegistry();
    var config = new ClaimsOutboxMetricsConfig();
    var repository =
        new com.pcis.observability.metrics.JdbcOutboxEventMetricsRepository(
            org.mockito.Mockito.mock(javax.sql.DataSource.class));
    var properties = new com.pcis.outbox.OutboxProperties();
    properties.setMetricsNamespace("claims_outbox");

    OutboxMetrics metrics =
        config.claimsOutboxMetrics(registry, repository, properties, new org.springframework.mock.env.MockEnvironment());

    metrics.refreshMetrics();
    assertThat(registry.find("claims_outbox_lag_seconds").gauge()).isNotNull();
    assertThat(registry.find("claims_outbox_publish_failures_total").counter()).isNotNull();
  }
}
