package com.pcis.billing.batch.cmm001b.job;

import com.pcis.billing.batch.cmm001b.config.CommissionCalculationProperties;
import com.pcis.billing.batch.cmm001b.infrastructure.CommissionLedgerWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommissionRunLogTasklet implements Tasklet {

  private static final String INSERT_RUN_LOG =
      """
      INSERT INTO RPT_RUN_LOG_T (
          PGM_NAME, RUN_DATE, REC_SELECTED, REC_UPDATED, REC_ERRORS,
          START_TIMESTAMP, END_TIMESTAMP, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final CommissionCalculationProperties properties;

  public CommissionRunLogTasklet(
      JdbcTemplate jdbcTemplate, CommissionCalculationProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ExecutionContext jobContext =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
    long calculated = jobContext.getLong(CommissionLedgerWriter.CALCULATED_COUNT_KEY, 0L);
    int errors =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getStepExecutions().stream()
            .mapToInt(step -> step.getFailureExceptions().size())
            .sum();

    Instant end = Instant.now();
    Instant start =
        chunkContext.getStepContext().getStepExecution().getJobExecution().getStartTime() != null
            ? chunkContext
                .getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getStartTime()
                .atZone(java.time.ZoneOffset.UTC)
                .toInstant()
            : end;

    jdbcTemplate.update(
        INSERT_RUN_LOG,
        properties.getProgramName(),
        Date.valueOf(properties.getReferenceDate()),
        (int) calculated,
        (int) calculated,
        errors,
        Timestamp.from(start),
        Timestamp.from(end),
        properties.getProgramName(),
        Timestamp.from(end));

    return RepeatStatus.FINISHED;
  }
}
