package com.pcis.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class BatchJobExitCodeMetricsTest {

  @Test
  void recordsExitCodePerJobName() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BatchJobExitCodeMetrics metrics = new BatchJobExitCodeMetrics(registry);

    metrics.recordExitCode("commission-calc-job", 2);

    assertThat(metrics.exitCodeFor("commission-calc-job")).isEqualTo(2.0);
    assertThat(
            registry
                .find(BatchJobExitCodeMetrics.METRIC_NAME)
                .tag("job_name", "commission-calc-job")
                .gauge()
                .value())
        .isEqualTo(2.0);
  }

  @Test
  void updatesExistingJobGauge() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BatchJobExitCodeMetrics metrics = new BatchJobExitCodeMetrics(registry);

    metrics.recordExitCode("audit-archive-job", 0);
    metrics.recordExitCode("audit-archive-job", 2);

    assertThat(metrics.exitCodeFor("audit-archive-job")).isEqualTo(2.0);
  }
}
