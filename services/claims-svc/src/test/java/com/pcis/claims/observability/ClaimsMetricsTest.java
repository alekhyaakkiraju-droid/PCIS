package com.pcis.claims.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClaimsMetricsTest {

  private SimpleMeterRegistry registry;
  private ClaimsMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new ClaimsMetrics(registry);
  }

  @Test
  void registersCoreMetricNames() {
    var sample = metrics.startApiRequest();
    metrics.recordApiRequest(sample, "GET", "/api/v1/claims", 200);

    assertThat(registry.find(ClaimsMetrics.API_DURATION).tags("method", "GET").timer())
        .isNotNull();
    assertThat(registry.find(ClaimsMetrics.BATCH_JOB_DURATION).gauge()).isNotNull();
    assertThat(registry.find(ClaimsMetrics.BATCH_ITEMS_PROCESSED).counter()).isNotNull();
  }

  @Test
  void recordsApiDurationAndErrors() {
    var sample = metrics.startApiRequest();
    metrics.recordApiRequest(sample, "GET", "/api/v1/claims", 500);

    assertThat(registry.find(ClaimsMetrics.API_DURATION).tags("method", "GET").timer().count())
        .isEqualTo(1);
    assertThat(registry.find(ClaimsMetrics.API_ERROR_RATE).tags("status_code", "500").counter()
            .count())
        .isEqualTo(1);
  }

  @Test
  void recordsBatchCompletion() {
    metrics.recordBatchCompletion(Duration.ofSeconds(42), 10, 2);

    assertThat(registry.find(ClaimsMetrics.BATCH_JOB_DURATION).gauge().value()).isEqualTo(42.0);
    assertThat(registry.find(ClaimsMetrics.BATCH_ITEMS_PROCESSED).counter().count())
        .isEqualTo(10.0);
    assertThat(registry.find(ClaimsMetrics.BATCH_ITEMS_SKIPPED).counter().count()).isEqualTo(2.0);
  }
}
