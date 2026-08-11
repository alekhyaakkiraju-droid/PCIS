package com.pcis.premium.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCalculationRequest(
    @NotBlank @Size(max = 4) String policyType,
    @NotBlank @Size(max = 2) String state,
    @Size(max = 12) String policyNumber) {}
