package com.pcis.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateClaimRequest(
    @Size(max = 12) String claimNbr,
    @NotBlank @Size(max = 12) String polNbr,
    @NotNull Integer custId,
    @NotNull LocalDate lossDate,
    @NotBlank @Size(max = 3) String claimType,
    @Size(max = 4000) String description) {}
