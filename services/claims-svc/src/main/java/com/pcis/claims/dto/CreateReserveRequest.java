package com.pcis.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateReserveRequest(
    @NotBlank @Size(max = 3) String reserveType,
    @NotNull @Positive BigDecimal approvedAmt,
    @Size(max = 200) String reason) {}
