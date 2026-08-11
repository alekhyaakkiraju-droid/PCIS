package com.pcis.batch.policy.processor;

import com.pcis.batch.policy.client.PremiumRatingClient;
import com.pcis.batch.policy.client.PremiumRatingResponse;
import com.pcis.batch.policy.domain.RatingDeclinedException;
import com.pcis.batch.policy.domain.RenewalCandidateRow;
import com.pcis.batch.policy.domain.RenewalDecision;
import com.pcis.batch.policy.domain.RenewalTermCalculator;
import com.pcis.batch.policy.infrastructure.RenewalPolicyWriter;
import java.math.BigDecimal;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class RenewalProcessor implements ItemProcessor<RenewalCandidateRow, RenewalDecision> {

  private final PremiumRatingClient premiumRatingClient;
  private StepExecution stepExecution;

  public RenewalProcessor(PremiumRatingClient premiumRatingClient) {
    this.premiumRatingClient = premiumRatingClient;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  public RenewalDecision process(RenewalCandidateRow candidate) {
    incrementSelected();
    PremiumRatingResponse rating = premiumRatingClient.rateRenewal(candidate);
    if (rating.isDeclined()) {
      throw new RatingDeclinedException(candidate.polNbr());
    }
    BigDecimal newPremium =
        rating.finalPremium() != null ? rating.finalPremium() : candidate.premAnnual();
    return new RenewalDecision(
        candidate,
        RenewalTermCalculator.renewalPolicyNumber(candidate.polNbr()),
        RenewalTermCalculator.newEffectiveDate(candidate.expDate()),
        RenewalTermCalculator.newExpirationDate(candidate.effDate(), candidate.expDate()),
        newPremium,
        rating.isReferral());
  }

  private void incrementSelected() {
    if (stepExecution == null) {
      return;
    }
    var jobContext = stepExecution.getJobExecution().getExecutionContext();
    long current = jobContext.getLong(RenewalPolicyWriter.SELECTED_COUNT_KEY, 0L);
    jobContext.putLong(RenewalPolicyWriter.SELECTED_COUNT_KEY, current + 1);
  }
}
