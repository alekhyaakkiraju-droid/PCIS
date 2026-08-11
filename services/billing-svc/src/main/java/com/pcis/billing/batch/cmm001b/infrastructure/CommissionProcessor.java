package com.pcis.billing.batch.cmm001b.infrastructure;

import com.pcis.billing.batch.cmm001b.domain.CommissionCalculator;
import com.pcis.billing.batch.cmm001b.domain.CommissionCandidateRow;
import com.pcis.billing.batch.cmm001b.domain.CommissionDecision;
import org.springframework.batch.item.ItemProcessor;

public class CommissionProcessor implements ItemProcessor<CommissionCandidateRow, CommissionDecision> {

  @Override
  public CommissionDecision process(CommissionCandidateRow item) {
    if (item.commRate() == null) {
      return new CommissionDecision(item, null, false);
    }
    return new CommissionDecision(
        item,
        CommissionCalculator.commissionAmount(item.amtPaid(), item.commRate()),
        true);
  }
}
