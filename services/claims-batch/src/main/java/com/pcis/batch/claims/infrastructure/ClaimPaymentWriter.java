package com.pcis.batch.claims.infrastructure;

import com.pcis.batch.claims.config.ClaimPaymentProperties;
import com.pcis.batch.claims.domain.ApprovedReserveRow;
import com.pcis.batch.claims.domain.ClaimPaymentCalculator;
import com.pcis.batch.common.OutboxEventWriter;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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

public class ClaimPaymentWriter implements ItemWriter<ApprovedReserveRow> {

  public static final String SELECTED_COUNT_KEY = "selectedCount";
  public static final String UPDATED_COUNT_KEY = "updatedCount";

  private static final String INSERT_PAYMENT =
      """
      INSERT INTO CLAIM_PAYMENT_T (
          CLAIM_ID, PAYMENT_AMT, PAYMENT_DATE, PAYMENT_STATUS, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, 'P', ?, ?)
      """;

  private static final String UPDATE_RESERVE =
      """
      UPDATE CLAIM_RESERVE_T
      SET RESERVE_STATUS = 'PD', UPD_USER = ?, UPD_TIMESTAMP = ?
      WHERE RESERVE_HIST_ID = ?
      """;

  private static final String INSERT_RECOVERY =
      """
      INSERT INTO RECOVERY_T (
          CLAIM_ID, RECOVERY_AMT, RECOVERY_STATUS, RECOVERY_DATE, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, 'OPEN', ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter outboxEventWriter;
  private final ClaimPaymentProperties properties;
  private StepExecution stepExecution;

  public ClaimPaymentWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter outboxEventWriter,
      ClaimPaymentProperties properties) {
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
  public void write(Chunk<? extends ApprovedReserveRow> chunk) {
    List<? extends ApprovedReserveRow> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    Instant now = Instant.now();
    LocalDate paymentDate = LocalDate.now();
    int updated = 0;

    for (ApprovedReserveRow row : items) {
      BigDecimal paymentAmt = ClaimPaymentCalculator.paymentAmount(row.reserveAmt());
      jdbcTemplate.update(
          INSERT_PAYMENT,
          row.claimId(),
          paymentAmt,
          Date.valueOf(paymentDate),
          properties.getProgramName(),
          Timestamp.from(now));
      jdbcTemplate.update(
          UPDATE_RESERVE,
          properties.getProgramName(),
          Timestamp.from(now),
          row.reserveHistId());

      if (ClaimPaymentCalculator.requiresReinsuranceRecovery(
          row.reserveAmt(), properties.getCessionThreshold())) {
        jdbcTemplate.update(
            INSERT_RECOVERY,
            row.claimId(),
            row.reserveAmt(),
            Date.valueOf(paymentDate),
            properties.getProgramName(),
            Timestamp.from(now));
      }

      writePaymentOutbox(row, paymentAmt, now);
      updated++;
    }

    incrementCounter(SELECTED_COUNT_KEY, updated);
    incrementCounter(UPDATED_COUNT_KEY, updated);
  }

  private void writePaymentOutbox(ApprovedReserveRow row, BigDecimal paymentAmt, Instant now) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("claimId", row.claimId());
    payload.put("paymentAmt", paymentAmt.toPlainString());
    payload.put("reserveHistId", row.reserveHistId());
    payload.put("timestamp", now.toString());

    outboxEventWriter.write(
        "claim-payment",
        row.claimId(),
        "ClaimPaymentProcessed",
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
