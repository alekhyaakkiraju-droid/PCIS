package com.pcis.authz.domain.decision;

import java.math.BigDecimal;

/** Result of a claim payment authority evaluation. */
public record AuthorityCheckResult(
    AuthorizationDecision decision,
    ReasonCode reasonCode,
    Long approvalId,
    String approverPrincipal,
    BigDecimal authorityLimitApplied,
    BigDecimal cumulativePaidToDate) {

  public static AuthorityCheckResult deny(ReasonCode reasonCode) {
    return new AuthorityCheckResult(
        AuthorizationDecision.DENY, reasonCode, null, null, null, null);
  }

  public static AuthorityCheckResult permit(
      Long approvalId,
      String approverPrincipal,
      BigDecimal authorityLimitApplied,
      BigDecimal cumulativePaidToDate) {
    return new AuthorityCheckResult(
        AuthorizationDecision.PERMIT,
        ReasonCode.PAYMENT_AUTHORITY_GRANTED,
        approvalId,
        approverPrincipal,
        authorityLimitApplied,
        cumulativePaidToDate);
  }
}
