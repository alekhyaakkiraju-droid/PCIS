package com.pcis.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateClaimRequest(
    @NotBlank @Size(max = 1) String claimStatus,
    LocalDate lossDate,
    @Size(max = 3) String claimType) {}
