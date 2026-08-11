package com.pcis.premium.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pcis.premium.config.BigDecimalStringSerializer;
import com.pcis.premium.config.MoneyStringSerializer;
import java.math.BigDecimal;
import java.util.List;

public record PremiumCalculationResponse(
    String calculationId,
    String returnCode,
    String underwritingDecision,
    @JsonSerialize(using = BigDecimalStringSerializer.class) BigDecimal compositeRiskScore,
    String riskTier,
    @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal baseRate,
    @JsonSerialize(using = BigDecimalStringSerializer.class) BigDecimal ratingFactor,
    @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal basePremium,
    List<RatingComponentLine> factors,
    List<RatingComponentLine> discounts,
    List<RatingComponentLine> surcharges,
    List<RatingComponentLine> taxes,
    @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal finalPremium,
    Long matchedRuleId,
    String matchedRuleText,
    List<String> installmentAmounts) {

  public record RatingComponentLine(
      String code,
      @JsonSerialize(using = BigDecimalStringSerializer.class) BigDecimal factor,
      @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal amount) {}
}
