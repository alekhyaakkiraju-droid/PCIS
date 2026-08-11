package com.pcis.reporting.metrics;

import com.pcis.reporting.config.ReportingDataSourceConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(name = ReportingDataSourceConfig.REPORTING_DATASOURCE)
public class ReplicaLagMetrics {
  static final String METRIC_NAME = "pcis_reporting_replica_lag_seconds";
  private static final Logger log = LoggerFactory.getLogger(ReplicaLagMetrics.class);
  private static final String LAG_QUERY =
      "SELECT COALESCE(EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp())), 0) AS lag_seconds";

  private final DataSource reportingDataSource;
  private final AtomicReference<Double> lagSeconds = new AtomicReference<>(0.0);

  public ReplicaLagMetrics(
      MeterRegistry registry,
      @Qualifier(ReportingDataSourceConfig.REPORTING_DATASOURCE) DataSource reportingDataSource) {
    this.reportingDataSource = reportingDataSource;
    Gauge.builder(METRIC_NAME, lagSeconds, AtomicReference::get)
        .description("Aurora reporting replica replication lag in seconds")
        .tag("service", "reporting-svc")
        .register(registry);
  }

  @Scheduled(fixedDelayString = "${pcis.reporting.replica-lag.poll-interval-ms:15000}")
  public void refreshLag() {
    queryLagSeconds().ifPresentOrElse(lagSeconds::set, () -> lagSeconds.set(0.0));
  }

  Optional<Double> queryLagSeconds() {
    try (var c = reportingDataSource.getConnection();
        var st = c.createStatement();
        var rs = st.executeQuery(LAG_QUERY)) {
      return rs.next() ? Optional.of(Math.max(0.0, rs.getDouble("lag_seconds"))) : Optional.of(0.0);
    } catch (Exception ex) {
      log.warn("Failed to query replica lag — retaining last known value={}", lagSeconds.get(), ex);
      return Optional.empty();
    }
  }

  public double lagSecondsValue() {
    return lagSeconds.get();
  }
}
