package com.pcis.premium.batch.prm005b.infrastructure;

import com.pcis.premium.batch.prm005b.config.DelinquencyAgingProperties;
import com.pcis.premium.batch.prm005b.domain.DelinquencyDecision;
import com.pcis.batch.common.OutboxEventWriter;
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

public class DelinquencyAgingWriter implements ItemWriter<DelinquencyDecision> {

  public static final String UPDATED_COUNT_KEY = "updatedCount";
  public static final String DELINQUENT_COUNT_KEY = "delinquentCount";

  private static final String UPDATE_STATUS =
      """
      UPDATE BILLING_SCHEDULE_T
      SET SCHED_STATUS = ?, UPD_USER = ?, UPD_TIMESTAMP = ?
      WHERE BILL_SCHED_ID = ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter outboxEventWriter;
  private final DelinquencyAgingProperties properties;
  private StepExecution stepExecution;

  public DelinquencyAgingWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter outboxEventWriter,
      DelinquencyAgingProperties properties) {
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
  public void write(Chunk<? extends DelinquencyDecision> chunk) {
    List<? extends DelinquencyDecision> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    Instant now = Instant.now();
    int updated = 0;
    int delinquent = 0;

    for (DelinquencyDecision decision : items) {
      jdbcTemplate.update(
          UPDATE_STATUS,
          decision.newStatus(),
          properties.getProgramName(),
          Timestamp.from(now),
          decision.candidate().billSchedId());

      if ("L".equals(decision.newStatus())) {
        delinquent++;
      }

      writeOutbox(decision, now);
      updated++;
    }

    incrementCounter(UPDATED_COUNT_KEY, updated);
    incrementCounter(DELINQUENT_COUNT_KEY, delinquent);
  }

  private void writeOutbox(DelinquencyDecision decision, Instant now) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("polNbr", decision.candidate().polNbr());
    payload.put("oldStatus", decision.candidate().schedStatus());
    payload.put("newStatus", decision.newStatus());
    payload.put("billSchedId", decision.candidate().billSchedId());
    payload.put("timestamp", now.toString());

    outboxEventWriter.write(
        "billing-schedule",
        decision.candidate().polNbr(),
        "DelinquencyStatusChanged",
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
