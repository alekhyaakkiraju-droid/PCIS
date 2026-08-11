package com.pcis.billing.batch.bil003b.infrastructure;

import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.batch.bil003b.domain.BillingInstallmentDecision;
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

public class BillingGenerationWriter implements ItemWriter<BillingInstallmentDecision> {

  public static final String GENERATED_COUNT_KEY = "generatedCount";

  private static final String INSERT_SCHEDULE =
      """
      INSERT INTO BILLING_SCHEDULE_T (
          POL_NBR, BILL_PLAN_ID, INSTALLMENT_NBR, DUE_DATE, AMT_DUE, AMT_PAID,
          SCHED_STATUS, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, 0, 'O', ?, ?)
      RETURNING BILL_SCHED_ID
      """;

  private static final String INSERT_INVOICE =
      """
      INSERT INTO INVOICE_T (
          BILL_SCHED_ID, INVOICE_DATE, INVOICE_DUE_DATE, INVOICE_AMT, INVOICE_STATUS,
          CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, 'OPEN', ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter outboxEventWriter;
  private final BillingGenerationProperties properties;
  private StepExecution stepExecution;

  public BillingGenerationWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter outboxEventWriter,
      BillingGenerationProperties properties) {
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
  public void write(Chunk<? extends BillingInstallmentDecision> chunk) {
    List<? extends BillingInstallmentDecision> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    Instant now = Instant.now();
    int generated = 0;

    for (BillingInstallmentDecision decision : items) {
      Long billSchedId =
          jdbcTemplate.queryForObject(
              INSERT_SCHEDULE,
              Long.class,
              decision.candidate().polNbr(),
              decision.candidate().billPlanId(),
              decision.installmentNbr(),
              Date.valueOf(decision.dueDate()),
              decision.amount(),
              properties.getProgramName(),
              Timestamp.from(now));

      jdbcTemplate.update(
          INSERT_INVOICE,
          billSchedId,
          Date.valueOf(decision.dueDate()),
          Date.valueOf(decision.dueDate()),
          decision.amount(),
          properties.getProgramName(),
          Timestamp.from(now));

      writeOutbox(decision, billSchedId, now);
      generated++;
    }

    incrementCounter(GENERATED_COUNT_KEY, generated);
  }

  private void writeOutbox(BillingInstallmentDecision decision, Long billSchedId, Instant now) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("polNbr", decision.candidate().polNbr());
    payload.put("installmentNbr", decision.installmentNbr());
    payload.put("amount", decision.amount().toPlainString());
    payload.put("billSchedId", billSchedId);
    payload.put("timestamp", now.toString());

    outboxEventWriter.write(
        "billing-schedule",
        decision.candidate().polNbr(),
        "InstallmentGenerated",
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
