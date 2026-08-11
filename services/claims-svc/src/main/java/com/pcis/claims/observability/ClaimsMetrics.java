package com.pcis.claims.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Domain Prometheus metrics for claims-svc API and batch observability (WO-200). */
@Component
public class ClaimsMetrics {

  public static final String API_DURATION = "claims_api_request_duration_seconds";
  public static final String API_ERROR_RATE = "claims_api_error_rate";
  public static final String BATCH_JOB_DURATION = "claims_batch_job_duration_seconds";
  public static final String BATCH_ITEMS_PROCESSED = "claims_batch_items_processed_total";
  public static final String BATCH_ITEMS_SKIPPED = "claims_batch_items_skipped_total";

  private final MeterRegistry registry;
  private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
  private final Map<String, Counter> skipCounters = new ConcurrentHashMap<>();
  private final AtomicReference<Double> batchJobDurationSeconds = new AtomicReference<>(0.0);
  private final Counter batchItemsProcessed;

  public ClaimsMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.batchItemsProcessed =
        Counter.builder(BATCH_ITEMS_PROCESSED)
            .description("Claim payment batch items processed")
            .register(registry);
    Gauge.builder(BATCH_JOB_DURATION, batchJobDurationSeconds, AtomicReference::get)
        .description("Most recent claim payment batch job duration in seconds")
        .register(registry);
  }

  public Timer.Sample startApiRequest() {
    return Timer.start(registry);
  }

  public void recordApiRequest(Timer.Sample sample, String method, String uri, int statusCode) {
    sample.stop(
        Timer.builder(API_DURATION)
            .description("Claims API request latency")
            .tag("method", method)
            .tag("uri", uri)
            .tag("status", String.valueOf(statusCode))
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry));
    if (statusCode >= 400) {
      errorCounter(String.valueOf(statusCode)).increment();
    }
  }

  public void recordBatchCompletion(Duration duration, long processed, long skipped) {
    batchJobDurationSeconds.set(duration.toMillis() / 1000.0);
    if (processed > 0) {
      batchItemsProcessed.increment(processed);
    }
    if (skipped > 0) {
      skipCounter("UNSPECIFIED").increment(skipped);
    }
  }

  public void recordBatchSkip(String reasonCode) {
    skipCounter(reasonCode).increment();
  }

  private Counter errorCounter(String statusCode) {
    return errorCounters.computeIfAbsent(
        statusCode,
        code ->
            Counter.builder(API_ERROR_RATE)
                .tag("status_code", code)
                .description("Claims API error responses")
                .register(registry));
  }

  private Counter skipCounter(String reasonCode) {
    return skipCounters.computeIfAbsent(
        reasonCode,
        reason ->
            Counter.builder(BATCH_ITEMS_SKIPPED)
                .tag("reason", reason)
                .description("Claim payment batch items skipped")
                .register(registry));
  }
}
