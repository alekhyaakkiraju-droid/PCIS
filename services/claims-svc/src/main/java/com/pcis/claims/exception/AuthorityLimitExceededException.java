package com.pcis.claims.exception;

import java.math.BigDecimal;

/** Raised when a payment exceeds the adjuster's authority limit. */
public class AuthorityLimitExceededException extends PaymentAuthorizationException {

  private final BigDecimal authorityLimit;
  private final BigDecimal requestedAmount;

  public AuthorityLimitExceededException(BigDecimal authorityLimit, BigDecimal requestedAmount) {
    super(
        String.format(
            "Payment amount %s exceeds authority limit %s",
            requestedAmount.toPlainString(), authorityLimit.toPlainString()));
    this.authorityLimit = authorityLimit;
    this.requestedAmount = requestedAmount;
  }

  public BigDecimal getAuthorityLimit() {
    return authorityLimit;
  }

  public BigDecimal getRequestedAmount() {
    return requestedAmount;
  }
}
