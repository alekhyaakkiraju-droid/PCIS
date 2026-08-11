package com.pcis.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.util.StringUtils;

/**
 * Exposes {@code pcis_batch_job_exit_code} gauge per {@code job_name} after batch completion.
 *
 * <p>Spring Batch jobs register exit codes via {@link BatchJobExitCodeListener} or call
 * {@link #recordExitCode(String, int)} directly before process exit.
 */
public class BatchJobExitCodeMetrics {

  static final String METRIC_NAME = "pcis_batch_job_exit_code";

  private final MeterRegistry registry;
  private final Map<String, AtomicReference<Double>> exitCodes = new ConcurrentHashMap<>();

  public BatchJobExitCodeMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  /** Record the terminal exit code for a batch job (0 = success, 1-5 = contract codes). */
  public void recordExitCode(String jobName, int exitCode) {
    if (!StringUtils.hasText(jobName)) {
      return;
    }
    AtomicReference<Double> holder =
        exitCodes.computeIfAbsent(
            jobName,
            name ->
                registerGauge(
                    name, new AtomicReference<>((double) exitCode)));
    holder.set((double) exitCode);
  }

  Double exitCodeFor(String jobName) {
    AtomicReference<Double> holder = exitCodes.get(jobName);
    return holder != null ? holder.get() : null;
  }

  private AtomicReference<Double> registerGauge(String jobName, AtomicReference<Double> holder) {
    Gauge.builder(METRIC_NAME, holder, AtomicReference::get)
        .description("Terminal Spring Batch job exit code (WO-137 contract)")
        .tags(Tags.of("job_name", jobName))
        .register(registry);
    return holder;
  }
}
