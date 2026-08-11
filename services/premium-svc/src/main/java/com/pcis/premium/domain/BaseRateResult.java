package com.pcis.premium.domain;

import java.math.BigDecimal;
import java.util.List;

public record BaseRateResult(
    BigDecimal baseRate,
    BigDecimal compositeFactor,
    BigDecimal basePremium,
    List<String> missingFactorTypes,
    RatingOutcome ratingOutcome) {

  public static BaseRateResult notFound() {
    return new BaseRateResult(null, null, null, List.of(), RatingOutcome.RATE_NOT_FOUND);
  }
}
