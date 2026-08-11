package com.pcis.sync.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SyncMetrics {

  private final MeterRegistry meterRegistry;
  private final Map<String, Counter> extractedCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> upsertedCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> failureCounters = new ConcurrentHashMap<>();

  public SyncMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordSuccess(String domainName, long rowsExtracted, long rowsUpserted) {
    extractedCounter(domainName).increment(rowsExtracted);
    upsertedCounter(domainName).increment(rowsUpserted);
    meterRegistry
        .counter("pcis.sync.runs", "domain", domainName, "status", "success")
        .increment();
  }

  public void recordFailure(String domainName) {
    failureCounter(domainName).increment();
    meterRegistry
        .counter("pcis.sync.runs", "domain", domainName, "status", "failed")
        .increment();
  }

  private Counter extractedCounter(String domainName) {
    return extractedCounters.computeIfAbsent(
        domainName,
        domain ->
            Counter.builder("pcis.sync.rows.extracted")
                .tag("domain", domain)
                .description("Rows extracted from source database")
                .register(meterRegistry));
  }

  private Counter upsertedCounter(String domainName) {
    return upsertedCounters.computeIfAbsent(
        domainName,
        domain ->
            Counter.builder("pcis.sync.rows.upserted")
                .tag("domain", domain)
                .description("Rows upserted to target database")
                .register(meterRegistry));
  }

  private Counter failureCounter(String domainName) {
    return failureCounters.computeIfAbsent(
        domainName,
        domain ->
            Counter.builder("pcis.sync.failures")
                .tag("domain", domain)
                .description("Sync run failures")
                .register(meterRegistry));
  }
}
