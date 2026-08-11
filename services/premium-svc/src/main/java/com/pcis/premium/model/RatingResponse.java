package com.pcis.premium.model;

import com.pcis.premium.domain.RatingOutcome;
import java.math.BigDecimal;
import java.util.List;

public record RatingResponse(
    String calculationId,
    RatingOutcome outcome,
    UnderwritingDecision underwritingDecision,
    String returnCode,
    BigDecimal compositeRiskScore,
    String riskTier,
    BigDecimal baseRate,
    BigDecimal ratingFactor,
    BigDecimal basePremium,
    BigDecimal discountPremium,
    BigDecimal surchargePremium,
    BigDecimal finalPremium,
    Long matchedRuleId,
    String matchedRuleText,
    List<PremiumDetailLine> detailLines,
    List<BigDecimal> installmentAmounts,
    boolean reinsuranceFlag) {

  public static RatingResponse invalidInput(String detail) {
    return new RatingResponse(
        null,
        RatingOutcome.INVALID_INPUT,
        UnderwritingDecision.DECLINE,
        RatingOutcome.INVALID_INPUT.returnCode(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        detail,
        List.of(),
        List.of(),
        false);
  }

  public static RatingResponse decline(
      String calculationId,
      Long matchedRuleId,
      String matchedRuleText,
      BigDecimal compositeRiskScore,
      String riskTier) {
    return new RatingResponse(
        calculationId,
        RatingOutcome.DECLINE,
        UnderwritingDecision.DECLINE,
        RatingOutcome.DECLINE.returnCode(),
        compositeRiskScore,
        riskTier,
        null,
        null,
        null,
        null,
        null,
        null,
        matchedRuleId,
        matchedRuleText,
        List.of(),
        List.of(),
        false);
  }
}
