package com.pcis.premium.domain;

/** Legacy return codes mapped for base-rate lookup (WO-187). */
public enum RatingOutcome {
  ACCEPT("00"),
  REFERRAL("01"),
  DECLINE("02"),
  RATE_NOT_FOUND("90"),
  INCOMPLETE_DATA("91"),
  INVALID_INPUT("99");

  private final String returnCode;

  RatingOutcome(String returnCode) {
    this.returnCode = returnCode;
  }

  public String returnCode() {
    return returnCode;
  }
}
