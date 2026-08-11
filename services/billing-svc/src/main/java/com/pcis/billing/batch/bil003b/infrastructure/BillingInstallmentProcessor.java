package com.pcis.billing.batch.bil003b.infrastructure;

import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.batch.bil003b.domain.BillingCandidateRow;
import com.pcis.billing.batch.bil003b.domain.BillingInstallmentDecision;
import com.pcis.billing.batch.bil003b.domain.FrequencyIntervalMapper;
import com.pcis.billing.batch.bil003b.domain.InstallmentCalculator;
import com.pcis.billing.batch.bil003b.domain.LeadWindowFilter;
import com.pcis.billing.batch.bil003b.exception.BusinessRuleException;
import com.pcis.billing.batch.bil003b.exception.TemporaryException;
import com.pcis.billing.config.BillingConfigProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class BillingInstallmentProcessor
    implements ItemProcessor<BillingCandidateRow, BillingInstallmentDecision> {

  private static final Logger log = LoggerFactory.getLogger(BillingInstallmentProcessor.class);

  private final BillingGenerationProperties properties;
  private final BillingConfigProperties billingConfig;

  public BillingInstallmentProcessor(
      BillingGenerationProperties properties, BillingConfigProperties billingConfig) {
    this.properties = properties;
    this.billingConfig = billingConfig;
  }

  @Override
  public BillingInstallmentDecision process(BillingCandidateRow item) {
    if (properties.getFailPolicyForTest() != null
        && properties.getFailPolicyForTest().equals(item.polNbr())) {
      throw new TemporaryException("Injected transient failure on " + item.polNbr());
    }
    validateCandidate(item);

    int nextInstallmentNbr = item.lastInstallmentNbr() + 1;
    LocalDate nextDue =
        FrequencyIntervalMapper.nextDueDate(
            item.lastDueDate(), item.billFreq(), properties.getReferenceDate());
    long daysOut = LeadWindowFilter.daysOut(nextDue, properties.getReferenceDate());
    if (!LeadWindowFilter.isEligible(daysOut, billingConfig.getLeadDays())) {
      return null;
    }

    BigDecimal amount =
        InstallmentCalculator.calculate(
            item.premAnnual(), item.installmentCnt(), nextInstallmentNbr);
    return new BillingInstallmentDecision(item, nextInstallmentNbr, nextDue, amount);
  }

  private void validateCandidate(BillingCandidateRow item) {
    if (item.billPlanId() <= 0) {
      throw new BusinessRuleException(
          item.polNbr(), "MISSING_BILLING_PLAN", "Active billing plan not found");
    }
    if (item.premAnnual() == null || item.premAnnual().signum() < 0) {
      throw new BusinessRuleException(
          item.polNbr(), "INVALID_PREMIUM", "Annual premium must be zero or positive");
    }
    if (item.installmentCnt() <= 0) {
      throw new BusinessRuleException(
          item.polNbr(), "INVALID_INSTALLMENT_COUNT", "Installment count must be positive");
    }
    if (item.lastInstallmentNbr() >= item.installmentCnt()) {
      throw new BusinessRuleException(
          item.polNbr(), "INSTALLMENTS_COMPLETE", "All installments already generated");
    }
    log.debug(
        "Processing policy {} installment {} of {}",
        item.polNbr(),
        item.lastInstallmentNbr() + 1,
        item.installmentCnt());
  }
}
