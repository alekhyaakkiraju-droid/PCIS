package com.pcis.premium.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCalculationRequest(
    @NotBlank @Size(max = 4) String policyType,
    @NotBlank @Size(max = 2) String state,
    @Size(max = 4) String coverageType,
    @Size(max = 3) String territory,
    @Size(max = 12) String policyNumber,
    @Pattern(regexp = "^-?\\d+(\\.\\d{1,2})?$") String limit,
    @Pattern(regexp = "^-?\\d+(\\.\\d{1,2})?$") String oldPremium,
    @Size(max = 1) String billingFrequency) {}
