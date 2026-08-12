package com.pcis.billing.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand execution of billing-svc's Spring Batch jobs. billing-svc keeps
 * spring.batch.job.enabled=false so these jobs don't auto-run when the web server boots — the
 * batch infrastructure (JobLauncher/JobRepository) is still a live bean regardless, so this
 * exposes a manual run path for local testing and demos.
 *
 * <p>Also captures the job's own log output for that specific run (via a temporary Logback
 * appender scoped to the batch packages) and, for jobs that INSERT new rows with a CRT_TIMESTAMP
 * column, queries back the rows that run actually created — real business-level output, not just
 * read/write counts.
 */
@RestController
@RequestMapping("/api/v1/billing/batch")
public class BatchTriggerController {

  private static final Set<String> TRIGGERABLE_JOBS =
      Set.of("billingGenerationJob", "commissionCalculationJob", "delinquencyAgingJob");
  private static final int RECORD_PREVIEW_LIMIT = 20;

  private final JobLauncher jobLauncher;
  private final JdbcTemplate jdbcTemplate;
  private final Job billingGenerationJob;
  private final Job commissionCalculationJob;
  private final Job delinquencyAgingJob;

  public BatchTriggerController(
      JobLauncher jobLauncher,
      JdbcTemplate jdbcTemplate,
      @Qualifier("billingGenerationJob") Job billingGenerationJob,
      @Qualifier("commissionCalculationJob") Job commissionCalculationJob,
      @Qualifier("delinquencyAgingJob") Job delinquencyAgingJob) {
    this.jobLauncher = jobLauncher;
    this.jdbcTemplate = jdbcTemplate;
    this.billingGenerationJob = billingGenerationJob;
    this.commissionCalculationJob = commissionCalculationJob;
    this.delinquencyAgingJob = delinquencyAgingJob;
  }

  @PostMapping("/{jobName}/run")
  @PreAuthorize("hasAuthority('billing:write')")
  public ResponseEntity<?> triggerRun(@PathVariable String jobName) {
    if (!TRIGGERABLE_JOBS.contains(jobName)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Unknown or non-triggerable job: " + jobName));
    }
    Job job = resolveJob(jobName);
    var params =
        new JobParametersBuilder().addLong("triggeredAt", System.currentTimeMillis()).toJobParameters();

    Instant startedAt = Instant.now();
    ListAppender<ILoggingEvent> appender = attachLogCapture();
    try {
      JobExecution execution = jobLauncher.run(job, params);
      List<String> logLines = detachLogCapture(appender);
      List<Map<String, Object>> createdRecords =
          execution.getStatus() == BatchStatus.COMPLETED
              ? fetchCreatedRecords(jobName, startedAt)
              : List.of();
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("jobName", jobName);
      body.put("jobExecutionId", execution.getId());
      body.put("status", execution.getStatus().toString());
      body.put("exitCode", execution.getExitStatus().getExitCode());
      body.put("logLines", logLines);
      body.put("createdRecords", createdRecords);
      return ResponseEntity.ok(body);
    } catch (JobExecutionException e) {
      detachLogCapture(appender);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /** Scoped to the batch packages, not root — keeps the capture to this job's own log lines. */
  private ListAppender<ILoggingEvent> attachLogCapture() {
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.getLogger("org.springframework.batch").addAppender(appender);
    context.getLogger("com.pcis.billing.batch").addAppender(appender);
    context.getLogger("com.pcis.batch").addAppender(appender);
    return appender;
  }

  private List<String> detachLogCapture(ListAppender<ILoggingEvent> appender) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.getLogger("org.springframework.batch").detachAppender(appender);
    context.getLogger("com.pcis.billing.batch").detachAppender(appender);
    context.getLogger("com.pcis.batch").detachAppender(appender);
    appender.stop();
    return appender.list.stream()
        .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
        .map(event -> "[%s] %s".formatted(event.getLevel(), event.getFormattedMessage()))
        .collect(Collectors.toList());
  }

  private List<Map<String, Object>> fetchCreatedRecords(String jobName, Instant startedAt) {
    String sql =
        switch (jobName) {
          case "billingGenerationJob" ->
              """
              SELECT POL_NBR AS "policyNumber", INSTALLMENT_NBR AS "installmentNumber",
                     DUE_DATE AS "dueDate", AMT_DUE AS "amountDue"
              FROM BILLING_SCHEDULE_T
              WHERE CRT_TIMESTAMP >= ?
              ORDER BY BILL_SCHED_ID DESC
              LIMIT ?
              """;
          case "commissionCalculationJob" ->
              """
              SELECT AGT_ID AS "agentId", BILL_SCHED_ID AS "billScheduleId",
                     COMM_RATE AS "commissionRate", COMMISSION_AMT AS "commissionAmount"
              FROM COMMISSION_LEDGER_T
              WHERE CRT_TIMESTAMP >= ?
              ORDER BY LEDGER_ID DESC
              LIMIT ?
              """;
          default -> null;
        };
    if (sql == null) {
      return List.of();
    }
    return jdbcTemplate.queryForList(sql, java.sql.Timestamp.from(startedAt), RECORD_PREVIEW_LIMIT);
  }

  private Job resolveJob(String jobName) {
    return switch (jobName) {
      case "billingGenerationJob" -> billingGenerationJob;
      case "commissionCalculationJob" -> commissionCalculationJob;
      case "delinquencyAgingJob" -> delinquencyAgingJob;
      default -> throw new IllegalStateException("Unreachable: " + jobName);
    };
  }
}
