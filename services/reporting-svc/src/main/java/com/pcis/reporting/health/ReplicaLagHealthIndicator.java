package com.pcis.reporting.health;

import com.pcis.reporting.config.ReportingDataSourceConfig;
import com.pcis.reporting.metrics.ReplicaLagMetrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component("replicaLag")
@ConditionalOnBean(name = ReportingDataSourceConfig.REPORTING_DATASOURCE)
public class ReplicaLagHealthIndicator implements HealthIndicator {
  static final double READINESS_THRESHOLD_SECONDS = 30.0;
  private final ReplicaLagMetrics replicaLagMetrics;

  public ReplicaLagHealthIndicator(ReplicaLagMetrics replicaLagMetrics) {
    this.replicaLagMetrics = replicaLagMetrics;
  }

  @Override
  public Health health() {
    replicaLagMetrics.refreshLag();
    double lag = replicaLagMetrics.lagSecondsValue();
    if (lag > READINESS_THRESHOLD_SECONDS) {
      return Health.down()
          .withDetail("replicaLagSeconds", lag)
          .withDetail("thresholdSeconds", READINESS_THRESHOLD_SECONDS)
          .build();
    }
    return Health.up().withDetail("replicaLagSeconds", lag).build();
  }
}
