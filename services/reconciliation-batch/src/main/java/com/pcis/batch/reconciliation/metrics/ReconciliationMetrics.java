package com.pcis.batch.reconciliation.metrics;

import com.pcis.batch.reconciliation.domain.BreakClass;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationMetrics {

  public static final String DIFF_COUNT = "pcis_reconciliation_diff_count";
  public static final String CONSECUTIVE_CLEAN_DAYS = "pcis_reconciliation_consecutive_clean_days";
  public static final String ROWS_COMPARED = "pcis_reconciliation_rows_compared_total";
  public static final String RUN_DURATION = "pcis_reconciliation_run_duration_seconds";

  private final MeterRegistry registry;
  private final Map<String, AtomicInteger> consecutiveCleanDays = new ConcurrentHashMap<>();
  private final Map<String, Counter> diffCounters = new ConcurrentHashMap<>();

  public ReconciliationMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordBreak(String domain, BreakClass breakClass) {
    diffCounter(domain, breakClass).increment();
    registry
        .counter(
            "pcis_reconciliation_breaks_total",
            "domain",
            domain,
            "classification",
            breakClass.name())
        .increment();
  }

  public void recordRowsCompared(String domain, long count) {
    registry
        .counter(ROWS_COMPARED, "domain", domain)
        .increment(count);
  }

  public Timer.Sample startRunTimer() {
    return Timer.start(registry);
  }

  public void recordRunDuration(String domain, Timer.Sample sample) {
    sample.stop(
        Timer.builder(RUN_DURATION)
            .tag("domain", domain)
            .description("Reconciliation run duration in seconds")
            .register(registry));
  }

  public void setConsecutiveCleanDays(String domain, int days) {
    consecutiveCleanDays
        .computeIfAbsent(
            domain,
            key -> {
              AtomicInteger holder = new AtomicInteger(days);
              Gauge.builder(CONSECUTIVE_CLEAN_DAYS, holder, AtomicInteger::get)
                  .tag("domain", domain)
                  .description("Consecutive days with zero unexplained reconciliation breaks")
                  .register(registry);
              return holder;
            })
        .set(days);
  }

  public void resetDiffCount(String domain) {
    for (BreakClass breakClass : BreakClass.values()) {
      String key = domain + ":" + breakClass.name();
      diffCounters.remove(key);
    }
  }

  private Counter diffCounter(String domain, BreakClass breakClass) {
    String key = domain + ":" + breakClass.name();
    return diffCounters.computeIfAbsent(
        key,
        ignored ->
            Counter.builder(DIFF_COUNT)
                .tag("domain", domain)
                .tag("break_class", breakClass.name())
                .description("Reconciliation diff count by domain and break class")
                .register(registry));
  }
}
