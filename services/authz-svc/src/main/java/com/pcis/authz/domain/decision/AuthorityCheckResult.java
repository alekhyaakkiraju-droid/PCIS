package com.pcis.authz.domain.decision;

import com.pcis.authz.util.PrincipalMaskingUtil;
import java.math.BigDecimal;

/** Result of a claim payment authority evaluation. */
public record AuthorityCheckResult(
    AuthorizationDecision decision,
    ReasonCode reasonCode,
    Long approvalId,
    String approverPrincipal,
    BigDecimal authorityLimitApplied,
    BigDecimal cumulativePaidToDate,
    String maskedApproverPrincipal,
    String maskedDisburserPrincipal) {

  public static AuthorityCheckResult deny(ReasonCode reasonCode) {
    return new AuthorityCheckResult(
        AuthorizationDecision.DENY, reasonCode, null, null, null, null, null, null);
  }

  public static AuthorityCheckResult denySod(
      ReasonCode reasonCode,
      Long approvalId,
      String approverPrincipal,
      String disburserPrincipal,
      BigDecimal authorityLimitApplied,
      BigDecimal cumulativePaidToDate) {
    return new AuthorityCheckResult(
        AuthorizationDecision.DENY,
        reasonCode,
        approvalId,
        approverPrincipal,
        authorityLimitApplied,
        cumulativePaidToDate,
        PrincipalMaskingUtil.maskPrincipal(approverPrincipal),
        PrincipalMaskingUtil.maskPrincipal(disburserPrincipal));
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
        cumulativePaidToDate,
        null,
        null);
  }
}
