package com.pcis.authz.application;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import com.pcis.authz.domain.decision.AuthorityCheckResult;
import com.pcis.authz.domain.decision.ReasonCode;
import com.pcis.authz.infrastructure.persistence.entity.ApprovalEntity;
import com.pcis.authz.infrastructure.persistence.projection.AdjusterAuthorityProjection;
import com.pcis.authz.infrastructure.persistence.projection.ReservePaidToDateProjection;
import com.pcis.authz.infrastructure.persistence.repository.ApprovalRepository;
import com.pcis.authz.infrastructure.persistence.repository.ClaimAdjusterRepository;
import com.pcis.authz.infrastructure.persistence.repository.ClaimReserveRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates claim payment authority: approval linkage (check one) then cumulative authority limit
 * (check two, BR-01 / P-B01).
 */
@Service
public class PaymentAuthorityService {

  private final ApprovalRepository approvalRepository;
  private final ClaimAdjusterRepository claimAdjusterRepository;
  private final ClaimReserveRepository claimReserveRepository;

  public PaymentAuthorityService(
      ApprovalRepository approvalRepository,
      ClaimAdjusterRepository claimAdjusterRepository,
      ClaimReserveRepository claimReserveRepository) {
    this.approvalRepository = approvalRepository;
    this.claimAdjusterRepository = claimAdjusterRepository;
    this.claimReserveRepository = claimReserveRepository;
  }

  @Transactional(readOnly = true)
  public AuthorityCheckResult checkPaymentAuthority(
      String claimId, Long reserveId, BigDecimal requestedAmount, String adjusterId) {
    if (isBlank(claimId) || reserveId == null || requestedAmount == null || isBlank(adjusterId)) {
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    var approval =
        approvalRepository.findFirstByClaimIdAndReserveHistIdAndApprovalStatus(
            claimId, reserveId, ApprovalEntity.STATUS_APPROVED);
    if (approval.isEmpty()) {
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    ApprovalEntity approved = approval.get();
    var reserve = claimReserveRepository.findByReserveHistIdAndClaimId(reserveId, claimId);
    if (reserve.isEmpty()) {
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    var adjuster = claimAdjusterRepository.findAuthorityByAdjusterId(adjusterId);
    if (adjuster.isEmpty()) {
      return AuthorityCheckResult.deny(ReasonCode.AUTHORITY_LIMIT_EXCEEDED);
    }

    return evaluateCumulativeLimit(approved, reserve.get(), adjuster.get(), requestedAmount);
  }

  @Transactional(readOnly = true)
  public AuthorizationResponse evaluate(
      String principalId, AuthorizationRequest request, String correlationId) {
    Map<String, Object> context = request.context();
    String claimId = stringValue(context.get("claimId"));
    Long reserveId = longValue(context.get("reserveId"));
    BigDecimal requestedAmount = decimalValue(context.get("requestedAmount"));
    String adjusterId = stringValue(context.get("adjusterId"));
    if (isBlank(adjusterId)) {
      adjusterId = principalId;
    }

    AuthorityCheckResult result =
        checkPaymentAuthority(claimId, reserveId, requestedAmount, adjusterId);
    return toAuthorizationResponse(result, correlationId);
  }

  private static AuthorityCheckResult evaluateCumulativeLimit(
      ApprovalEntity approval,
      ReservePaidToDateProjection reserve,
      AdjusterAuthorityProjection adjuster,
      BigDecimal requestedAmount) {
    BigDecimal paidToDate = PaymentAuthorityCalculator.normalize(reserve.getPaidToDate());
    BigDecimal cumulative =
        PaymentAuthorityCalculator.cumulativePayout(paidToDate, requestedAmount);
    BigDecimal authorityLimit =
        PaymentAuthorityCalculator.normalize(adjuster.getAuthorityLimit());

    if (PaymentAuthorityCalculator.exceedsAuthorityLimit(cumulative, authorityLimit)) {
      return AuthorityCheckResult.deny(ReasonCode.AUTHORITY_LIMIT_EXCEEDED);
    }

    return AuthorityCheckResult.permit(
        approval.getApprovalId(),
        approval.getApproverId(),
        authorityLimit,
        cumulative);
  }

  private static AuthorizationResponse toAuthorizationResponse(
      AuthorityCheckResult result, String correlationId) {
    List<String> evaluatedPermissions = new ArrayList<>();
    if (result.approvalId() != null) {
      evaluatedPermissions.add("approval:" + result.approvalId());
    }
    if (result.approverPrincipal() != null) {
      evaluatedPermissions.add("approver:" + result.approverPrincipal());
    }
    if (result.authorityLimitApplied() != null) {
      evaluatedPermissions.add("authorityLimit:" + result.authorityLimitApplied());
    }
    if (result.cumulativePaidToDate() != null) {
      evaluatedPermissions.add("cumulativePaid:" + result.cumulativePaidToDate());
    }
    return new AuthorizationResponse(
        result.decision(), result.reasonCode(), List.copyOf(evaluatedPermissions), correlationId);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String stringValue(Object value) {
    return value == null ? null : value.toString();
  }

  private static Long longValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(value.toString());
  }

  private static BigDecimal decimalValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return new BigDecimal(value.toString());
  }
}
