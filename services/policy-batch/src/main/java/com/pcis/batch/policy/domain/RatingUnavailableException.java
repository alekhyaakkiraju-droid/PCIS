package com.pcis.batch.policy.domain;

public class RatingUnavailableException extends RuntimeException {

  public static final String REASON_CODE = "RATING_UNAVAILABLE";

  public RatingUnavailableException(String polNbr, Throwable cause) {
    super("Premium rating unavailable for policy " + polNbr, cause);
  }
}
