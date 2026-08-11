package com.pcis.config;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TunableRow(
    String key,
    String valueType,
    String valueText,
    BigDecimal numericValue,
    BigDecimal minValue,
    BigDecimal maxValue,
    LocalDate effectiveFrom,
    LocalDate effectiveTo) {}
