package com.pcis.claims.exception;

import com.pcis.error.ValidationException;

public class PolicyNotInForceException extends ValidationException {

  public PolicyNotInForceException(String polNbr, String detail) {
    super(
        "Loss date is outside every in-force period for policy " + polNbr + ": " + detail,
        "system",
        "policy:" + polNbr,
        "fnol");
  }
}
