package com.pcis.policy.batch.pol006b.client;

import java.math.BigDecimal;

public record RatingResponse(
    String calculationId,
    String returnCode,
    String underwritingDecision,
    BigDecimal finalPremium) {

  public boolean isDeclined() {
    return "02".equals(returnCode) || "DECLINE".equalsIgnoreCase(underwritingDecision);
  }

  public boolean isReferral() {
    return "01".equals(returnCode) || "REFER".equalsIgnoreCase(underwritingDecision);
  }

  public boolean isInvalidInput() {
    return "99".equals(returnCode);
  }
}
