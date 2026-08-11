package com.pcis.policy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PolicyCreateRequest(
    @NotNull Integer customerId,
    @NotBlank @Size(max = 8) String agentId,
    @NotBlank @Pattern(regexp = "[A-Z0-9\\-]{2,4}") String policyType,
    @NotNull @DecimalMin("0.01") BigDecimal annualPremium,
    @NotNull LocalDate effectiveDate,
    @NotNull LocalDate expirationDate,
    @NotNull @Size(min = 1) @Valid List<CoverageRequest> coverages,
    @NotNull @Valid BillingPlanRequest billingPlan) {

  public record CoverageRequest(
      @NotBlank @Pattern(regexp = "[A-Z0-9\\-]{2,4}") String coverageType,
      @NotNull @DecimalMin("0.01") BigDecimal coverageLimit,
      @NotNull @DecimalMin("0.00") BigDecimal premiumAmount,
      @Valid List<DeductibleRequest> deductibles) {}

  public record DeductibleRequest(
      @NotBlank @Pattern(regexp = "[A-Z0-9]{2,4}") String deductibleType,
      @NotNull @DecimalMin("0.00") BigDecimal deductibleAmount) {}

  public record BillingPlanRequest(
      @NotBlank @Pattern(regexp = "[MQSA]") String billingFrequency,
      @NotNull Integer installmentCount) {}
}
