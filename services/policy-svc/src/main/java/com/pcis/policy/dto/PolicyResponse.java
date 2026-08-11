package com.pcis.policy.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponse(
    String policyNumber,
    Integer customerId,
    String agentId,
    String policyType,
    String status,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    BigDecimal annualPremium,
    List<CoverageResponse> coverages,
    BillingPlanResponse billingPlan,
    List<HistoryResponse> history,
    Instant createdAt,
    Instant updatedAt) {

  public record CoverageResponse(
      String coverageId,
      String coverageType,
      BigDecimal coverageLimit,
      BigDecimal deductibleAmount,
      BigDecimal premiumAmount,
      List<DeductibleResponse> deductibles) {}

  public record DeductibleResponse(String deductibleType, BigDecimal deductibleAmount) {}

  public record BillingPlanResponse(
      String billingFrequency, Integer installmentCount, BigDecimal installmentFee) {}

  public record HistoryResponse(
      String eventCode, LocalDate eventDate, String eventDescription) {}
}
