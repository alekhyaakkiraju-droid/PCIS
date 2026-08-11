package com.pcis.policy.batch.pol006b.infrastructure;

import com.pcis.batch.common.BatchJobExecutionListener;
import com.pcis.policy.batch.pol006b.config.PolicyRenewalProperties;
import com.pcis.policy.batch.pol006b.domain.RenewalResult;
import com.pcis.policy.batch.pol006b.exception.AuditFailureException;
import com.pcis.policy.domain.entity.CoverageEntity;
import com.pcis.policy.domain.entity.DeductibleEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.repository.BillingPlanRepository;
import com.pcis.policy.domain.repository.CoverageRepository;
import com.pcis.policy.domain.repository.DeductibleRepository;
import com.pcis.policy.domain.repository.PolicyHistoryRepository;
import com.pcis.policy.domain.repository.PolicyRepository;
import com.pcis.policy.outbox.PolicyOutboxWriter;
import java.util.Map;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RenewalWriter implements ItemWriter<RenewalResult> {

  public static final String RENEWED_COUNT_KEY = "policyRenewalRenewedCount";

  private static final String STATUS_RENEWED = "RNED";

  private final PolicyRepository policyRepository;
  private final CoverageRepository coverageRepository;
  private final DeductibleRepository deductibleRepository;
  private final BillingPlanRepository billingPlanRepository;
  private final PolicyHistoryRepository policyHistoryRepository;
  private final PolicyOutboxWriter policyOutboxWriter;
  private final PolicyRenewalProperties properties;
  private StepExecution stepExecution;

  public RenewalWriter(
      PolicyRepository policyRepository,
      CoverageRepository coverageRepository,
      DeductibleRepository deductibleRepository,
      BillingPlanRepository billingPlanRepository,
      PolicyHistoryRepository policyHistoryRepository,
      PolicyOutboxWriter policyOutboxWriter,
      PolicyRenewalProperties properties) {
    this.policyRepository = policyRepository;
    this.coverageRepository = coverageRepository;
    this.deductibleRepository = deductibleRepository;
    this.billingPlanRepository = billingPlanRepository;
    this.policyHistoryRepository = policyHistoryRepository;
    this.policyOutboxWriter = policyOutboxWriter;
    this.properties = properties;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  @Transactional
  public void write(Chunk<? extends RenewalResult> chunk) {
    for (RenewalResult result : chunk.getItems()) {
      persistRenewal(result);
      incrementRenewedCount();
    }
  }

  private void persistRenewal(RenewalResult result) {
    policyRepository.save(result.renewalPolicy());

    for (CoverageEntity coverage : result.renewalCoverages()) {
      coverageRepository.save(coverage);
      for (DeductibleEntity deductible : coverage.getDeductibles()) {
        deductibleRepository.save(deductible);
      }
    }

    billingPlanRepository.save(result.renewalBillingPlan());
    policyHistoryRepository.save(result.renewalHistory());

    PolicyEntity source = result.sourcePolicy();
    source.setPolStatus(STATUS_RENEWED);
    policyRepository.save(source);

    writeRenewalOutbox(result);
  }

  private void writeRenewalOutbox(RenewalResult result) {
    try {
      policyOutboxWriter.writeDomainEvent(
          result.renewalPolicy().getPolNbr(),
          "PolicyRenewed",
          Map.of(
              "program", properties.getProgramName(),
              "sourcePolicy", result.sourcePolicy().getPolNbr(),
              "renewalPolicy", result.renewalPolicy().getPolNbr(),
              "newPremium", result.renewalPolicy().getPremAnnual().toPlainString(),
              "referralFlag", result.referralFlag()),
          result.idempotencyKey());
    } catch (DataAccessException ex) {
      markOutboxFailure();
      throw new AuditFailureException(
          "PolicyRenewed outbox write failed for " + result.renewalPolicy().getPolNbr(), ex);
    }
  }

  private void markOutboxFailure() {
    if (stepExecution != null) {
      stepExecution
          .getJobExecution()
          .getExecutionContext()
          .put(BatchJobExecutionListener.OUTBOX_WRITE_FAILED_KEY, Boolean.TRUE);
    }
  }

  private void incrementRenewedCount() {
    if (stepExecution == null) {
      return;
    }
    var jobContext = stepExecution.getJobExecution().getExecutionContext();
    long current = jobContext.getLong(RENEWED_COUNT_KEY, 0L);
    jobContext.putLong(RENEWED_COUNT_KEY, current + 1);
  }
}
