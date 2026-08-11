package com.pcis.billing.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record PaymentRequest(
    @NotBlank String polNbr,
    @NotBlank String custId,
    @NotBlank
        @DecimalMin("0.01")
        @DecimalMax("99999999999.99")
        @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "paymentAmt must be a decimal with up to 2 scale")
        String paymentAmt,
    @NotBlank @Pattern(regexp = "CH|AC|WI") String paymentMethod,
    @NotNull LocalDate paymentDate,
    @NotBlank String paymentToken) {}
