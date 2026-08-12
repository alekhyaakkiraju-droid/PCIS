package com.pcis.policy.dto;

import com.pcis.policy.domain.entity.BillingPlanEntity;
import com.pcis.policy.domain.entity.CoverageEntity;
import com.pcis.policy.domain.entity.DeductibleEntity;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.entity.PolicyHistoryEntity;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PolicyMapper {

  public PolicyResponse toResponse(PolicyEntity policy) {
    return new PolicyResponse(
        policy.getPolNbr(),
        policy.getCustId(),
        policy.getAgtId(),
        trimCode(policy.getPolicyType()),
        toApiStatus(policy.getPolStatus()),
        policy.getEffDate(),
        policy.getExpDate(),
        policy.getPremAnnual(),
        policy.getCoverages().stream().map(this::toCoverageResponse).toList(),
        policy.getBillingPlan() != null ? toBillingPlanResponse(policy.getBillingPlan()) : null,
        policy.getHistory().stream()
            .sorted(Comparator.comparing(PolicyHistoryEntity::getEventDate))
            .map(this::toHistoryResponse)
            .toList(),
        policy.getCrtTimestamp(),
        policy.getUpdTimestamp());
  }

  /** List view — avoids lazy-loading collections outside a persistence context. */
  public PolicyResponse toSummaryResponse(PolicyEntity policy) {
    return new PolicyResponse(
        policy.getPolNbr(),
        policy.getCustId(),
        policy.getAgtId(),
        trimCode(policy.getPolicyType()),
        toApiStatus(policy.getPolStatus()),
        policy.getEffDate(),
        policy.getExpDate(),
        policy.getPremAnnual(),
        List.of(),
        null,
        List.of(),
        policy.getCrtTimestamp(),
        policy.getUpdTimestamp());
  }

  public PolicyListResponse toListResponse(List<PolicyResponse> content, int page, int size, long total) {
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
    return new PolicyListResponse(content, new PolicyListResponse.PageMetadata(page, size, total, totalPages));
  }

  public static String toApiStatus(String dbStatus) {
    if (dbStatus == null) {
      return null;
    }
    return switch (dbStatus.trim()) {
      case "NEW" -> "NEW";
      case "ACTV" -> "ACTIVE";
      case "CANC" -> "CANCELLED";
      default -> dbStatus.trim();
    };
  }

  public static String toDbStatus(String apiStatus) {
    if (apiStatus == null || apiStatus.isBlank()) {
      return null;
    }
    return switch (apiStatus.trim().toUpperCase()) {
      case "NEW" -> "NEW ";
      case "ACTIVE" -> "ACTV";
      case "CANCELLED" -> "CANC";
      default -> apiStatus;
    };
  }

  public static String padChar4(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() >= 4) {
      return trimmed.substring(0, 4);
    }
    return String.format("%-4s", trimmed);
  }

  private PolicyResponse.CoverageResponse toCoverageResponse(CoverageEntity coverage) {
    return new PolicyResponse.CoverageResponse(
        coverage.getCoverageId(),
        trimCode(coverage.getCovType()),
        coverage.getLimitAmt(),
        coverage.getDedAmt(),
        coverage.getCovPremium(),
        coverage.getDeductibles().stream().map(this::toDeductibleResponse).toList());
  }

  private PolicyResponse.DeductibleResponse toDeductibleResponse(DeductibleEntity deductible) {
    return new PolicyResponse.DeductibleResponse(
        trimCode(deductible.getDedType()), deductible.getDedAmt());
  }

  private PolicyResponse.BillingPlanResponse toBillingPlanResponse(BillingPlanEntity plan) {
    return new PolicyResponse.BillingPlanResponse(
        trimCode(plan.getBillFreq()),
        plan.getNbrInstallments().intValue(),
        plan.getInstallmentFee());
  }

  private PolicyResponse.HistoryResponse toHistoryResponse(PolicyHistoryEntity history) {
    return new PolicyResponse.HistoryResponse(
        trimCode(history.getEventCode()),
        history.getEventDate(),
        history.getEventDesc());
  }

  private static String trimCode(String value) {
    return value == null ? null : value.trim();
  }
}
