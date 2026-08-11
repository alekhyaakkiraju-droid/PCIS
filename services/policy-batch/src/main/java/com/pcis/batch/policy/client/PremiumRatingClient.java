package com.pcis.batch.policy.client;

import com.pcis.batch.policy.domain.RenewalCandidateRow;

public interface PremiumRatingClient {

  PremiumRatingResponse rateRenewal(RenewalCandidateRow candidate);
}
