package com.pcis.batch.claims.config;

import com.pcis.batch.claims.domain.SkipReasonCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/** Records claims batch Prometheus metrics (WO-200) from claimPaymentJob executions. */
@Component
public class ClaimPaymentMetricsListener implements JobExecutionListener, StepExecutionListener {

  static final String BATCH_JOB_DURATION = "claims_batch_job_duration_seconds";
  static final String BATCH_ITEMS_PROCESSED = "claims_batch_items_processed_total";
  static final String BATCH_ITEMS_SKIPPED = "claims_batch_items_skipped_total";

  private final MeterRegistry registry;
  private final AtomicReference<Double> jobDurationSeconds = new AtomicReference<>(0.0);
  private final Counter itemsProcessed;
  private final Map<String, Counter> skipCounters = new ConcurrentHashMap<>();
  private Instant jobStart;

  public ClaimPaymentMetricsListener(MeterRegistry registry) {
    this.registry = registry;
    this.itemsProcessed =
        Counter.builder(BATCH_ITEMS_PROCESSED)
            .description("Claim payment batch items processed")
            .register(registry);
    Gauge.builder(BATCH_JOB_DURATION, jobDurationSeconds, AtomicReference::get)
        .description("Most recent claim payment batch job duration in seconds")
        .register(registry);
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    jobStart = Instant.now();
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobStart != null) {
      jobDurationSeconds.set(
          Duration.between(jobStart, Instant.now()).toMillis() / 1000.0);
    }
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    if (!"claimPaymentStep".equals(stepExecution.getStepName())) {
      return stepExecution.getExitStatus();
    }
    long writeCount = stepExecution.getWriteCount();
    if (writeCount > 0) {
      itemsProcessed.increment(writeCount);
    }
    long skipCount = stepExecution.getSkipCount();
    if (skipCount > 0) {
      skipCounter(SkipReasonCode.NO_APPROVAL.name()).increment(skipCount);
    }
    return stepExecution.getExitStatus();
  }

  private Counter skipCounter(String reason) {
    return skipCounters.computeIfAbsent(
        reason,
        r ->
            Counter.builder(BATCH_ITEMS_SKIPPED)
                .tag("reason", r)
                .description("Claim payment batch items skipped")
                .register(registry));
  }
}
