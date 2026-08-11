package com.pcis.authz.application;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import com.pcis.authz.domain.decision.AuthorityCheckResult;
import com.pcis.authz.domain.decision.AuthorizationDecision;
import com.pcis.authz.domain.decision.PaymentOperations;
import com.pcis.authz.domain.decision.ReasonCode;
import com.pcis.authz.infrastructure.persistence.entity.ApprovalEntity;
import com.pcis.authz.infrastructure.persistence.projection.AdjusterAuthorityProjection;
import com.pcis.authz.infrastructure.persistence.projection.ReservePaidToDateProjection;
import com.pcis.authz.infrastructure.persistence.repository.ApprovalRepository;
import com.pcis.authz.infrastructure.persistence.repository.ClaimAdjusterRepository;
import com.pcis.authz.infrastructure.persistence.repository.ClaimReserveRepository;
import com.pcis.authz.util.PrincipalMaskingUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates claim payment authority: approval linkage (check one), cumulative authority limit
 * (check two, BR-01 / P-B01), then segregation-of-duties (check three, BR-01 / SOX).
 */
@Service
public class PaymentAuthorityService {

  private static final Logger log = LoggerFactory.getLogger(PaymentAuthorityService.class);

  static final String ROLE_BATCH_SVC = "BATCH_SVC";
  static final String ROLE_BATCH = "BATCH";

  private final ApprovalRepository approvalRepository;
  private final ClaimAdjusterRepository claimAdjusterRepository;
  private final ClaimReserveRepository claimReserveRepository;
  private final PermissionResolver permissionResolver;

  public PaymentAuthorityService(
      ApprovalRepository approvalRepository,
      ClaimAdjusterRepository claimAdjusterRepository,
      ClaimReserveRepository claimReserveRepository,
      PermissionResolver permissionResolver) {
    this.approvalRepository = approvalRepository;
    this.claimAdjusterRepository = claimAdjusterRepository;
    this.claimReserveRepository = claimReserveRepository;
    this.permissionResolver = permissionResolver;
  }

  @Transactional(readOnly = true)
  public AuthorityCheckResult checkPaymentAuthority(
      String claimId, Long reserveId, BigDecimal requestedAmount, String disburserPrincipal) {
    if (isBlank(claimId)
        || reserveId == null
        || requestedAmount == null
        || isBlank(disburserPrincipal)) {
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    var approval =
        approvalRepository.findFirstByClaimIdAndReserveHistIdAndApprovalStatus(
            claimId, reserveId, ApprovalEntity.STATUS_APPROVED);
    if (approval.isEmpty()) {
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    ApprovalEntity approved = approval.get();
    String approverPrincipal = approved.getApproverId();
    if (isBlank(approverPrincipal)) {
      log.error(
          "Approval record missing approver for claimId={} reserveId={}", claimId, reserveId);
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    var reserve = claimReserveRepository.findByReserveHistIdAndClaimId(reserveId, claimId);
    if (reserve.isEmpty()) {
      return AuthorityCheckResult.deny(ReasonCode.APPROVAL_MISSING);
    }

    var adjuster = claimAdjusterRepository.findAuthorityByAdjusterId(disburserPrincipal);
    if (adjuster.isEmpty()) {
      return AuthorityCheckResult.deny(ReasonCode.AUTHORITY_LIMIT_EXCEEDED);
    }

    AuthorityCheckResult limitResult =
        evaluateCumulativeLimit(approved, reserve.get(), adjuster.get(), requestedAmount);
    if (limitResult.decision() == AuthorizationDecision.DENY) {
      return limitResult;
    }

    if (samePrincipal(approverPrincipal, disburserPrincipal)) {
      log.warn(
          "Self-approval forbidden for claimId={} reserveId={} approver={} disburser={}",
          claimId,
          reserveId,
          PrincipalMaskingUtil.maskPrincipal(approverPrincipal),
          PrincipalMaskingUtil.maskPrincipal(disburserPrincipal));
      return AuthorityCheckResult.denySod(
          ReasonCode.SELF_APPROVAL_FORBIDDEN,
          approved.getApprovalId(),
          approverPrincipal,
          disburserPrincipal,
          limitResult.authorityLimitApplied(),
          limitResult.cumulativePaidToDate());
    }

    return limitResult;
  }

  @Transactional(readOnly = true)
  public AuthorizationResponse evaluate(
      String principalId, AuthorizationRequest request, String correlationId) {
    if (PaymentOperations.APPROVE_PAYMENT.equalsIgnoreCase(request.operation())) {
      return evaluateApprovePayment(principalId, request, correlationId);
    }

    Map<String, Object> context = request.context();
    String claimId = stringValue(context.get("claimId"));
    Long reserveId = longValue(context.get("reserveId"));
    BigDecimal requestedAmount = decimalValue(context.get("requestedAmount"));
    String disburserPrincipal = stringValue(context.get("adjusterId"));
    if (isBlank(disburserPrincipal)) {
      disburserPrincipal = principalId;
    }

    AuthorityCheckResult result =
        checkPaymentAuthority(claimId, reserveId, requestedAmount, disburserPrincipal);
    return toAuthorizationResponse(result, correlationId, claimId, reserveId);
  }

  private AuthorizationResponse evaluateApprovePayment(
      String principalId, AuthorizationRequest request, String correlationId) {
    if (hasBatchServiceRole(principalId)) {
      log.warn("Batch service account attempted approval: principal={}", principalId);
      return denyWithSodMetadata(
          ReasonCode.BATCH_CANNOT_APPROVE,
          principalId,
          stringValue(request.context().get("originalRequesterId")),
          correlationId,
          stringValue(request.context().get("claimId")),
          longValue(request.context().get("reserveId")));
    }

    String originalRequesterId = stringValue(request.context().get("originalRequesterId"));
    if (!isBlank(originalRequesterId) && samePrincipal(principalId, originalRequesterId)) {
      log.warn(
          "Self-approval forbidden on APPROVE_PAYMENT: approver={} requester={}",
          PrincipalMaskingUtil.maskPrincipal(principalId),
          PrincipalMaskingUtil.maskPrincipal(originalRequesterId));
      return denyWithSodMetadata(
          ReasonCode.SELF_APPROVAL_FORBIDDEN,
          principalId,
          originalRequesterId,
          correlationId,
          stringValue(request.context().get("claimId")),
          longValue(request.context().get("reserveId")));
    }

    return new AuthorizationResponse(
        AuthorizationDecision.PERMIT,
        ReasonCode.GRANT_MATCH,
        List.of("claim:approve_payment"),
        correlationId);
  }

  private boolean hasBatchServiceRole(String principalId) {
    return permissionResolver.resolveRoleCodes(principalId).stream()
        .anyMatch(
            code ->
                ROLE_BATCH_SVC.equalsIgnoreCase(code) || ROLE_BATCH.equalsIgnoreCase(code));
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
      AuthorityCheckResult result, String correlationId, String claimId, Long reserveId) {
    List<String> evaluatedPermissions = new ArrayList<>();
    appendSodMetadata(evaluatedPermissions, result, claimId, reserveId);
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

  private static AuthorizationResponse denyWithSodMetadata(
      ReasonCode reasonCode,
      String approverPrincipal,
      String disburserPrincipal,
      String correlationId,
      String claimId,
      Long reserveId) {
    List<String> evaluatedPermissions = new ArrayList<>();
    evaluatedPermissions.add(
        "sod:maskedApprover=" + PrincipalMaskingUtil.maskPrincipal(approverPrincipal));
    evaluatedPermissions.add(
        "sod:maskedDisburser=" + PrincipalMaskingUtil.maskPrincipal(disburserPrincipal));
    if (!isBlank(claimId)) {
      evaluatedPermissions.add("sod:claimId=" + claimId);
    }
    if (reserveId != null) {
      evaluatedPermissions.add("sod:reserveId=" + reserveId);
    }
    return new AuthorizationResponse(
        AuthorizationDecision.DENY, reasonCode, List.copyOf(evaluatedPermissions), correlationId);
  }

  static void appendSodMetadata(
      List<String> evaluatedPermissions,
      AuthorityCheckResult result,
      String claimId,
      Long reserveId) {
    if (result.maskedApproverPrincipal() != null) {
      evaluatedPermissions.add("sod:maskedApprover=" + result.maskedApproverPrincipal());
    }
    if (result.maskedDisburserPrincipal() != null) {
      evaluatedPermissions.add("sod:maskedDisburser=" + result.maskedDisburserPrincipal());
    }
    if (!isBlank(claimId)) {
      evaluatedPermissions.add("sod:claimId=" + claimId);
    }
    if (reserveId != null) {
      evaluatedPermissions.add("sod:reserveId=" + reserveId);
    }
  }

  private static boolean samePrincipal(String left, String right) {
    return left != null && right != null && left.equalsIgnoreCase(right);
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
