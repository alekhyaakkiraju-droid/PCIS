package com.pcis.billing.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
  private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

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
      List<String> logLines = detachLogCapture(appender);
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("error", e.getMessage());
      body.put("logLines", logLines);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
  }

  /**
   * Streams the job's log lines to the client as they are actually emitted (Server-Sent
   * Events), so the console shows real progress rather than a block of text that only appears
   * once the run has already finished. The job itself runs on a background thread since
   * jobLauncher.run(...) blocks until completion.
   */
  @GetMapping(value = "/{jobName}/run/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAuthority('billing:write')")
  public SseEmitter triggerRunStream(@PathVariable String jobName) {
    SseEmitter emitter = new SseEmitter(60_000L);
    if (!TRIGGERABLE_JOBS.contains(jobName)) {
      sendQuietly(emitter, "failed", "Unknown or non-triggerable job: " + jobName);
      emitter.complete();
      return emitter;
    }
    Thread.ofVirtual().start(() -> runAndStream(jobName, emitter));
    return emitter;
  }

  private void runAndStream(String jobName, SseEmitter emitter) {
    Job job = resolveJob(jobName);
    var params =
        new JobParametersBuilder().addLong("triggeredAt", System.currentTimeMillis()).toJobParameters();
    Instant startedAt = Instant.now();

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    StreamingLogAppender appender = new StreamingLogAppender(emitter);
    appender.start();
    attachAppender(context, appender);
    try {
      JobExecution execution = jobLauncher.run(job, params);
      detachAppender(context, appender);
      List<Map<String, Object>> createdRecords =
          execution.getStatus() == BatchStatus.COMPLETED
              ? fetchCreatedRecords(jobName, startedAt)
              : List.of();
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("jobName", jobName);
      result.put("jobExecutionId", execution.getId());
      result.put("status", execution.getStatus().toString());
      result.put("exitCode", execution.getExitStatus().getExitCode());
      result.put("createdRecords", createdRecords);
      emitter.send(SseEmitter.event().name("result").data(result));
      emitter.complete();
    } catch (JobExecutionException e) {
      detachAppender(context, appender);
      sendQuietly(emitter, "failed", e.getMessage());
      emitter.completeWithError(e);
    } catch (IOException e) {
      detachAppender(context, appender);
      emitter.completeWithError(e);
    }
  }

  private void sendQuietly(SseEmitter emitter, String eventName, String data) {
    try {
      emitter.send(SseEmitter.event().name(eventName).data(data));
    } catch (IOException ignored) {
      // Client disconnected — nothing left to notify.
    }
  }

  /** Pushes each qualifying log event straight to the SSE stream as it is logged. */
  private static final class StreamingLogAppender extends AppenderBase<ILoggingEvent> {
    private final SseEmitter emitter;

    StreamingLogAppender(SseEmitter emitter) {
      this.emitter = emitter;
    }

    @Override
    protected void append(ILoggingEvent event) {
      if (!event.getLevel().isGreaterOrEqual(Level.INFO)) {
        return;
      }
      try {
        emitter.send(SseEmitter.event().name("log").data(formatLogLine(event)));
      } catch (IOException e) {
        // Client disconnected mid-run — let the job keep running to completion regardless.
      }
    }
  }

  private static String formatLogLine(ILoggingEvent event) {
    return "%s [%s] %s"
        .formatted(
            LOG_TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp())),
            event.getLevel(),
            event.getFormattedMessage());
  }

  private static void attachAppender(LoggerContext context, Appender<ILoggingEvent> appender) {
    context.getLogger("org.springframework.batch").addAppender(appender);
    context.getLogger("com.pcis.billing.batch").addAppender(appender);
    context.getLogger("com.pcis.batch").addAppender(appender);
  }

  private static void detachAppender(LoggerContext context, Appender<ILoggingEvent> appender) {
    context.getLogger("org.springframework.batch").detachAppender(appender);
    context.getLogger("com.pcis.billing.batch").detachAppender(appender);
    context.getLogger("com.pcis.batch").detachAppender(appender);
    appender.stop();
  }

  /** Scoped to the batch packages, not root — keeps the capture to this job's own log lines. */
  private ListAppender<ILoggingEvent> attachLogCapture() {
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    attachAppender((LoggerContext) LoggerFactory.getILoggerFactory(), appender);
    return appender;
  }

  private List<String> detachLogCapture(ListAppender<ILoggingEvent> appender) {
    detachAppender((LoggerContext) LoggerFactory.getILoggerFactory(), appender);
    return appender.list.stream()
        .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
        .map(BatchTriggerController::formatLogLine)
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
