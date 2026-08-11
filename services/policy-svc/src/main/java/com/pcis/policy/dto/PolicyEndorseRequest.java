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

public record PolicyEndorseRequest(
    @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,10}") String endorsementType,
    @NotNull LocalDate effectiveDate,
    @Valid List<PolicyCreateRequest.CoverageRequest> coverageChanges,
    @NotBlank @Size(max = 100) String reason) {}
