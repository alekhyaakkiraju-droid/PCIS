package com.pcis.premium.service;

import com.pcis.premium.model.RatingRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Stub risk scoring (WO-189). Full PRMRSK01 implementation deferred to a future story.
 */
@Service
public class RiskScoreService {

  private static final BigDecimal DEFAULT_SCORE = new BigDecimal("35.0000");
  private static final String DEFAULT_TIER = "B";

  public RiskScoreResult computeCompositeRiskScore(RatingRequest request) {
    return new RiskScoreResult(DEFAULT_SCORE, DEFAULT_TIER);
  }

  public record RiskScoreResult(BigDecimal compositeScore, String riskTier) {}
}
