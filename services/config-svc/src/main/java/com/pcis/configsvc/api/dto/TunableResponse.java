package com.pcis.configsvc.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TunableResponse(
    String key,
    String domain,
    String valueType,
    String valueText,
    BigDecimal numericValue,
    BigDecimal minValue,
    BigDecimal maxValue,
    String unit,
    String description,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Integer version) {}
