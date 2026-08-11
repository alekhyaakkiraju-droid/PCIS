package com.pcis.premium.batch.prm005b.infrastructure;

import com.pcis.premium.batch.prm005b.config.DelinquencyAgingProperties;
import com.pcis.premium.batch.prm005b.domain.DelinquencyAgingCalculator;
import com.pcis.premium.batch.prm005b.domain.DelinquencyCandidateRow;
import com.pcis.premium.batch.prm005b.domain.DelinquencyDecision;
import org.springframework.batch.item.ItemProcessor;

public class DelinquencyAgingProcessor
    implements ItemProcessor<DelinquencyCandidateRow, DelinquencyDecision> {

  private final DelinquencyAgingProperties properties;

  public DelinquencyAgingProcessor(DelinquencyAgingProperties properties) {
    this.properties = properties;
  }

  @Override
  public DelinquencyDecision process(DelinquencyCandidateRow item) {
    String newStatus =
        DelinquencyAgingCalculator.computeStatus(
            item.amtDue(),
            item.amtPaid(),
            item.daysPastDue(),
            properties.getGraceDays());
    if (!DelinquencyAgingCalculator.statusChanged(item.schedStatus(), newStatus)) {
      return null;
    }
    return new DelinquencyDecision(item, newStatus);
  }
}
