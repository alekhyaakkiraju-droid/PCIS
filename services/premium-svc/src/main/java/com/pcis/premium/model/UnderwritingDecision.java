package com.pcis.premium.model;

import com.pcis.premium.domain.RatingOutcome;

public enum UnderwritingDecision {
  APPROVE,
  REFER,
  DECLINE;

  public static UnderwritingDecision fromOutcome(RatingOutcome outcome) {
    return switch (outcome) {
      case ACCEPT -> APPROVE;
      case REFERRAL -> REFER;
      case DECLINE -> DECLINE;
      default -> APPROVE;
    };
  }
}
