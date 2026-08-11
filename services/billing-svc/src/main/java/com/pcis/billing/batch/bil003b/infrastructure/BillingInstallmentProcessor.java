package com.pcis.billing.batch.bil003b.infrastructure;

import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.batch.bil003b.domain.BillingCandidateRow;
import com.pcis.billing.batch.bil003b.domain.BillingInstallmentCalculator;
import com.pcis.billing.batch.bil003b.domain.BillingInstallmentDecision;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.batch.item.ItemProcessor;

public class BillingInstallmentProcessor
    implements ItemProcessor<BillingCandidateRow, BillingInstallmentDecision> {

  private final BillingGenerationProperties properties;

  public BillingInstallmentProcessor(BillingGenerationProperties properties) {
    this.properties = properties;
  }

  @Override
  public BillingInstallmentDecision process(BillingCandidateRow item) {
    LocalDate nextDue =
        BillingInstallmentCalculator.nextDueDate(
            item.lastDueDate(), item.billFreq(), properties.getReferenceDate());
    long daysOut = BillingInstallmentCalculator.daysOut(nextDue, properties.getReferenceDate());
    if (!BillingInstallmentCalculator.withinLeadWindow(daysOut, properties.getLeadDays())) {
      return null;
    }
    BigDecimal amount =
        BillingInstallmentCalculator.installmentAmount(item.premAnnual(), item.installmentCnt());
    return new BillingInstallmentDecision(item, item.lastInstallmentNbr() + 1, nextDue, amount);
  }
}
