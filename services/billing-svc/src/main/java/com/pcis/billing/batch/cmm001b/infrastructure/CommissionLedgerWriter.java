package com.pcis.billing.batch.cmm001b.infrastructure;

import com.pcis.billing.batch.cmm001b.config.CommissionCalculationProperties;
import com.pcis.billing.batch.cmm001b.domain.CommissionDecision;
import com.pcis.batch.common.OutboxEventWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class CommissionLedgerWriter implements ItemWriter<CommissionDecision> {

  public static final String CALCULATED_COUNT_KEY = "calculatedCount";
  public static final String NO_PLAN_COUNT_KEY = "noPlanCount";

  private static final String INSERT_LEDGER =
      """
      INSERT INTO COMMISSION_LEDGER_T (
          AGT_ID, BILL_SCHED_ID, COMM_RATE, COMMISSION_AMT, CALC_DATE, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String UPDATE_FLAG =
      """
      UPDATE BILLING_SCHEDULE_T
      SET COMM_CALC_FLAG = 'Y', UPD_USER = ?, UPD_TIMESTAMP = ?
      WHERE BILL_SCHED_ID = ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter outboxEventWriter;
  private final CommissionCalculationProperties properties;
  private StepExecution stepExecution;

  public CommissionLedgerWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter outboxEventWriter,
      CommissionCalculationProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.outboxEventWriter = outboxEventWriter;
    this.properties = properties;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  @Transactional
  public void write(Chunk<? extends CommissionDecision> chunk) {
    List<? extends CommissionDecision> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    Instant now = Instant.now();
    int calculated = 0;
    int noPlan = 0;

    for (CommissionDecision decision : items) {
      if (!decision.hasPlan()) {
        noPlan++;
        continue;
      }

      jdbcTemplate.update(
          INSERT_LEDGER,
          decision.candidate().agtId(),
          decision.candidate().billSchedId(),
          decision.candidate().commRate(),
          decision.commissionAmount(),
          Date.valueOf(properties.getReferenceDate()),
          properties.getProgramName(),
          Timestamp.from(now));

      jdbcTemplate.update(
          UPDATE_FLAG,
          properties.getProgramName(),
          Timestamp.from(now),
          decision.candidate().billSchedId());

      writeOutbox(decision, now);
      calculated++;
    }

    incrementCounter(CALCULATED_COUNT_KEY, calculated);
    incrementCounter(NO_PLAN_COUNT_KEY, noPlan);
  }

  private void writeOutbox(CommissionDecision decision, Instant now) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("polNbr", decision.candidate().polNbr());
    payload.put("commissionAmt", decision.commissionAmount().toPlainString());
    payload.put("billSchedId", decision.candidate().billSchedId());
    payload.put("timestamp", now.toString());

    outboxEventWriter.write(
        "commission-ledger",
        decision.candidate().polNbr(),
        "CommissionCalculated",
        payload,
        UUID.randomUUID());
  }

  private void incrementCounter(String key, int delta) {
    if (stepExecution == null || delta == 0) {
      return;
    }
    var jobContext = stepExecution.getJobExecution().getExecutionContext();
    long current = jobContext.getLong(key, 0L);
    jobContext.putLong(key, current + delta);
  }
}
