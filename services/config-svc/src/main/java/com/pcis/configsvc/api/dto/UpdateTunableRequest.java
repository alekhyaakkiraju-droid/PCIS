package com.pcis.configsvc.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTunableRequest(
    BigDecimal numericValue,
    String valueText,
    @NotNull LocalDate effectiveFrom,
    @NotNull Integer expectedVersion,
    @NotBlank @Size(min = 5, max = 200) String changeReason) {}
