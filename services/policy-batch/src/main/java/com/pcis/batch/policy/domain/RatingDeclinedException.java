package com.pcis.batch.policy.domain;

public class RatingDeclinedException extends RuntimeException {

  public static final String REASON_CODE = "RATING_DECLINED";

  public RatingDeclinedException(String polNbr) {
    super("Premium rating declined for policy " + polNbr);
  }
}
