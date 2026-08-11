package com.pcis.premium.service;

import com.pcis.premium.domain.BaseRateResult;
import com.pcis.premium.domain.PremiumRatingMath;
import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.infrastructure.RateTableRepository;
import com.pcis.premium.infrastructure.RateTableRepository.RateFactorRow;
import com.pcis.premium.infrastructure.RateTableRepository.RateTableRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Base rate and composite factor lookup (WO-187, legacy PRMCLC01 section 3000). */
@Service
public class BaseRateService {

  private static final Logger log = LoggerFactory.getLogger(BaseRateService.class);
  private static final BigDecimal NEUTRAL_FACTOR = new BigDecimal("1.0000");
  private static final int FACTOR_SCALE = 4;
  private static final int PREMIUM_SCALE = 2;

  private static final Map<String, List<String>> EXPECTED_FACTORS =
      Map.of(
          "HOM", List.of("AGE", "TERR", "CLAIMS", "RISK-TIER"),
          "HOME", List.of("AGE", "TERR", "CLAIMS", "RISK-TIER"),
          "AUT", List.of("AGE", "TERR", "CLAIMS"),
          "CML", List.of("AGE", "TERR", "CLAIMS", "RISK-TIER", "OCCUPANCY"));

  private final RateTableRepository rateTableRepository;

  public BaseRateService(RateTableRepository rateTableRepository) {
    this.rateTableRepository = rateTableRepository;
  }

  public BaseRateResult computeBasePremium(
      String policyType, String coverageCode, String territoryCode, LocalDate effectiveDate) {
    validateInputs(policyType, territoryCode, effectiveDate);

    var rateTable =
        rateTableRepository
            .findEffectiveRateTable(normalizePolicyType(policyType), territoryCode, effectiveDate)
            .orElse(null);
    if (rateTable == null) {
      return BaseRateResult.notFound();
    }

    List<RateFactorRow> factorRows =
        rateTableRepository.loadFactorsForRateTable(rateTable.rateTableId());
    Map<String, BigDecimal> factorsByType = indexFactors(factorRows);
    List<String> expected =
        EXPECTED_FACTORS.getOrDefault(
            normalizePolicyType(policyType),
            List.of("AGE", "TERR", "CLAIMS", "RISK-TIER"));
    List<String> missing = new ArrayList<>();
    List<BigDecimal> multipliers = new ArrayList<>();

    for (String factorType : expected) {
      BigDecimal value = factorsByType.get(factorType);
      if (value == null) {
        missing.add(factorType);
        multipliers.add(NEUTRAL_FACTOR);
        log.warn(
            "Missing rating factor type={} policyType={} territory={} — using neutral 1.0000",
            factorType,
            policyType,
            territoryCode);
      } else {
        multipliers.add(value.setScale(FACTOR_SCALE, RoundingMode.HALF_UP));
      }
    }

    BigDecimal compositeFactor =
        PremiumRatingMath.combineFactors(multipliers).setScale(FACTOR_SCALE, RoundingMode.HALF_UP);
    BigDecimal basePremium =
        rateTable
            .baseRate()
            .multiply(compositeFactor)
            .setScale(PREMIUM_SCALE, RoundingMode.HALF_UP);

    return new BaseRateResult(
        rateTable.baseRate(),
        compositeFactor,
        basePremium,
        List.copyOf(missing),
        RatingOutcome.ACCEPT);
  }

  private static Map<String, BigDecimal> indexFactors(List<RateFactorRow> rows) {
    Map<String, BigDecimal> indexed = new LinkedHashMap<>();
    for (RateFactorRow row : rows) {
      indexed.put(row.factorCode().trim().toUpperCase(), row.factorValue());
    }
    return indexed;
  }

  private static void validateInputs(
      String policyType, String territoryCode, LocalDate effectiveDate) {
    if (policyType == null || policyType.isBlank()) {
      throw new IllegalArgumentException("policyType is required");
    }
    if (territoryCode == null || territoryCode.isBlank()) {
      throw new IllegalArgumentException("territoryCode is required");
    }
    if (effectiveDate == null) {
      throw new IllegalArgumentException("effectiveDate is required");
    }
  }

  private static String normalizePolicyType(String policyType) {
    return policyType.trim().toUpperCase();
  }
}
