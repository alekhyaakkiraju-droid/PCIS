package com.pcis.claims.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentRequest(
    @NotNull Long reserveId,
    @NotNull
        @DecimalMin(value = "0.01")
        @DecimalMax(value = "99999999999.99")
        @Digits(integer = 11, fraction = 2)
        BigDecimal amount,
    Integer payeeId) {}
