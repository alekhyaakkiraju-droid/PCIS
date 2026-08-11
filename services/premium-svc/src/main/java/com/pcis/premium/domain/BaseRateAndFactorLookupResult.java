package com.pcis.premium.domain;

import com.pcis.premium.infrastructure.RateTableRepository.RateFactorRow;
import java.math.BigDecimal;
import java.util.List;

public record BaseRateAndFactorLookupResult(
    long rateTableId,
    BigDecimal baseRate,
    List<RateFactorRow> factors,
    BigDecimal combinedFactor,
    BigDecimal basePremium) {}
