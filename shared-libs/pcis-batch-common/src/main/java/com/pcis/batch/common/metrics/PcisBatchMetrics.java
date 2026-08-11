package com.pcis.batch.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/** Custom batch metrics referenced by WO-213 Grafana dashboard panels. */
@Component
@ConditionalOnBean(MeterRegistry.class)
public class PcisBatchMetrics {

  public static final String ITEMS_SKIPPED = "pcis_batch_items_skipped_total";
  public static final String OUTBOX_LAG = "pcis_batch_outbox_lag_seconds";
  public static final String EXIT_CODE = "pcis_batch_exit_code";

  private final MeterRegistry registry;
  private final AtomicLong outboxLagSeconds = new AtomicLong(0);
  private final AtomicInteger exitCode = new AtomicInteger(0);

  public PcisBatchMetrics(MeterRegistry registry) {
    this.registry = registry;
    Gauge.builder(OUTBOX_LAG, outboxLagSeconds, AtomicLong::get)
        .description("Oldest unpublished outbox event age in seconds")
        .register(registry);
    Gauge.builder(EXIT_CODE, exitCode, AtomicInteger::get)
        .description("Last recorded batch exit code for the active job")
        .register(registry);
  }

  public void recordSkippedItems(String jobName, long count) {
    Counter.builder(ITEMS_SKIPPED)
        .tag("job_name", jobName)
        .description("Items skipped by Spring Batch processors")
        .register(registry)
        .increment(count);
  }

  public void setOutboxLagSeconds(long seconds) {
    outboxLagSeconds.set(seconds);
  }

  public void setExitCode(int code) {
    exitCode.set(code);
  }
}
