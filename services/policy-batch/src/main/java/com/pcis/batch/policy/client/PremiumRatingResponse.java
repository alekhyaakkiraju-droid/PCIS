package com.pcis.batch.policy.client;

import java.math.BigDecimal;

public record PremiumRatingResponse(
    String calculationId,
    String returnCode,
    String underwritingDecision,
    BigDecimal finalPremium) {

  public boolean isDeclined() {
    return "02".equals(returnCode) || "DECLINE".equals(underwritingDecision);
  }

  public boolean isReferral() {
    return "REFER".equals(underwritingDecision);
  }
}
