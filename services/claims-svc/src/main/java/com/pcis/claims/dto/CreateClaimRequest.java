package com.pcis.claims.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClaimRequest(
    @Size(max = 12) String claimNbr,
    @NotBlank @Size(max = 12) String polNbr,
    @NotNull Integer custId,
    @NotNull LocalDate lossDate,
    @NotBlank @Size(max = 3) String claimType,
    @Size(max = 4000) String description,
    @Size(max = 3) String initialReserveType,
    @DecimalMin(value = "0.01")
        @Digits(integer = 11, fraction = 2)
        BigDecimal initialReserveAmt) {}
