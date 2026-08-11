package com.pcis.premium.application;

import com.pcis.premium.config.PremiumRatingProperties;
import com.pcis.premium.domain.BaseRateAndFactorLookupResult;
import com.pcis.premium.domain.PremiumRatingMath;
import com.pcis.premium.domain.RateLookupNotFoundException;
import com.pcis.premium.infrastructure.RateTableRepository;
import com.pcis.premium.infrastructure.RateTableRepository.RateFactorRow;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PremiumRatingService {

  private final RateTableRepository rateTableRepository;
  private final PremiumRatingProperties properties;

  public PremiumRatingService(
      RateTableRepository rateTableRepository, PremiumRatingProperties properties) {
    this.rateTableRepository = rateTableRepository;
    this.properties = properties;
  }

  public void ensureReadPathWired(String calculationId) {
    if (calculationId == null || calculationId.isBlank()) {
      throw new IllegalArgumentException("calculationId is required");
    }
  }

  public BaseRateAndFactorLookupResult lookupBaseRateAndFactors(
      String policyType, String territory) {
    validateLookupInputs(policyType, territory);

    var rateTable =
        rateTableRepository
            .findEffectiveRateTable(policyType, territory)
            .orElseThrow(() -> new RateLookupNotFoundException(policyType, territory));

    List<RateFactorRow> factors =
        rateTableRepository.loadFactorsForRateTable(rateTable.rateTableId());
    BigDecimal combinedFactor =
        PremiumRatingMath.combineFactors(
            factors.stream().map(RateFactorRow::factorValue).toList());
    BigDecimal basePremium =
        PremiumRatingMath.multiplyAndRound(
            rateTable.baseRate(), combinedFactor, properties.getDecimalScale());

    return new BaseRateAndFactorLookupResult(
        rateTable.rateTableId(), rateTable.baseRate(), factors, combinedFactor, basePremium);
  }

  private static void validateLookupInputs(String policyType, String territory) {
    if (policyType == null || policyType.isBlank()) {
      throw new IllegalArgumentException("policyType is required");
    }
    if (territory == null || territory.isBlank()) {
      throw new IllegalArgumentException("territory is required");
    }
  }
}
