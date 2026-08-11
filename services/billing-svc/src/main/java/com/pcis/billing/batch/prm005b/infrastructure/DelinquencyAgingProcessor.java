package com.pcis.billing.batch.prm005b.infrastructure;

import com.pcis.billing.batch.bil003b.exception.BusinessRuleException;
import com.pcis.billing.batch.bil003b.exception.TemporaryException;
import com.pcis.billing.batch.prm005b.config.DelinquencyAgingProperties;
import com.pcis.billing.batch.prm005b.domain.DelinquencyCandidateRow;
import com.pcis.billing.batch.prm005b.domain.DelinquencyDecisionEngine;
import com.pcis.billing.batch.prm005b.domain.DelinquencyUpdate;
import com.pcis.billing.config.BillingConfigProperties;
import org.springframework.batch.item.ItemProcessor;

public class DelinquencyAgingProcessor implements ItemProcessor<DelinquencyCandidateRow, DelinquencyUpdate> {

  private final BillingConfigProperties billingConfig;
  private final DelinquencyAgingProperties properties;

  public DelinquencyAgingProcessor(
      BillingConfigProperties billingConfig, DelinquencyAgingProperties properties) {
    this.billingConfig = billingConfig;
    this.properties = properties;
  }

  @Override
  public DelinquencyUpdate process(DelinquencyCandidateRow item) {
    if (item.dueDate() == null) {
      throw new BusinessRuleException(
          item.polNbr(), "MISSING_DUE_DATE", "Installment due date is required");
    }
    Long failId = properties.getFailBillSchedIdForTest();
    if (failId != null && failId == item.billSchedId()) {
      throw new TemporaryException("Injected transient failure on schedule " + item.billSchedId());
    }

    return DelinquencyDecisionEngine.evaluate(
            item.amtDue(),
            item.amtPaid(),
            item.daysPastDue(),
            billingConfig.getGraceDays(),
            item.schedStatus())
        .map(transition -> new DelinquencyUpdate(item, transition))
        .orElse(null);
  }
}
