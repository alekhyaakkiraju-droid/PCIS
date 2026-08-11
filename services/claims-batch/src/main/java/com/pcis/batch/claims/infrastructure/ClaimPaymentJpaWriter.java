package com.pcis.batch.claims.infrastructure;

import com.pcis.batch.claims.config.ClaimPaymentProperties;
import com.pcis.batch.claims.domain.ClaimPaymentBatchItem;
import com.pcis.batch.claims.domain.ClaimPaymentCalculator;
import com.pcis.claims.domain.ClaimPaymentEntity;
import com.pcis.claims.domain.ClaimReserveEntity;
import com.pcis.claims.domain.RecoveryEntity;
import com.pcis.claims.domain.repository.ClaimPaymentRepository;
import com.pcis.claims.domain.repository.ClaimReserveRepository;
import com.pcis.claims.domain.repository.RecoveryRepository;
import com.pcis.claims.outbox.ClaimsOutboxWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

public class ClaimPaymentJpaWriter implements ItemWriter<ClaimPaymentBatchItem> {

  public static final String SELECTED_COUNT_KEY = "selectedCount";
  public static final String UPDATED_COUNT_KEY = "updatedCount";

  private final ClaimPaymentRepository claimPaymentRepository;
  private final ClaimReserveRepository claimReserveRepository;
  private final RecoveryRepository recoveryRepository;
  private final ClaimsOutboxWriter claimsOutboxWriter;
  private final ClaimPaymentProperties properties;
  private StepExecution stepExecution;

  public ClaimPaymentJpaWriter(
      ClaimPaymentRepository claimPaymentRepository,
      ClaimReserveRepository claimReserveRepository,
      RecoveryRepository recoveryRepository,
      ClaimsOutboxWriter claimsOutboxWriter,
      ClaimPaymentProperties properties) {
    this.claimPaymentRepository = claimPaymentRepository;
    this.claimReserveRepository = claimReserveRepository;
    this.recoveryRepository = recoveryRepository;
    this.claimsOutboxWriter = claimsOutboxWriter;
    this.properties = properties;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  @Transactional
  public void write(Chunk<? extends ClaimPaymentBatchItem> chunk) {
    List<? extends ClaimPaymentBatchItem> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    int updated = 0;
    for (ClaimPaymentBatchItem item : items) {
      ClaimReserveEntity reserve = item.reserve();
      BigDecimal paymentAmt = item.paymentAmount();
      String claimNbr = reserve.getClaim().getClaimNbr();

      ClaimPaymentEntity payment = new ClaimPaymentEntity();
      payment.setClaim(reserve.getClaim());
      payment.setPaymentAmt(paymentAmt);
      payment.setPaymentStatus("I");
      payment.setApproval(item.approval());
      payment.setAdjuster(item.adjuster());
      ClaimPaymentEntity saved = claimPaymentRepository.save(payment);

      reserve.setPaidToDate(reserve.getPaidToDate().add(paymentAmt));
      if (reserve.getPaidToDate().compareTo(reserve.getApprovedAmt()) >= 0) {
        reserve.setReserveStatus("P");
      }
      claimReserveRepository.save(reserve);

      if (ClaimPaymentCalculator.requiresReinsuranceRecovery(
          paymentAmt, properties.getCessionThreshold())) {
        RecoveryEntity recovery = new RecoveryEntity();
        recovery.setClaim(reserve.getClaim());
        recovery.setRecoveryAmt(paymentAmt);
        recovery.setRecoveryType("REI");
        recoveryRepository.save(recovery);
      }

      Map<String, Object> payload = new HashMap<>();
      payload.put("claimNbr", claimNbr);
      payload.put("paymentId", saved.getPaymentId());
      payload.put("paymentAmt", paymentAmt);
      payload.put("disburser", properties.getBatchServicePrincipal());
      claimsOutboxWriter.writeDomainEvent(
          claimNbr, "PaymentDisbursed", payload, UUID.randomUUID());
      updated++;
    }

    incrementCounter(SELECTED_COUNT_KEY, updated);
    incrementCounter(UPDATED_COUNT_KEY, updated);
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
