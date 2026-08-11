package com.pcis.premium.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.infrastructure.RateTableRepository;
import com.pcis.premium.infrastructure.RateTableRepository.RateFactorRow;
import com.pcis.premium.infrastructure.RateTableRepository.RateTableRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseRateServiceTest {

  @Mock private RateTableRepository rateTableRepository;
  private BaseRateService baseRateService;

  @BeforeEach
  void setUp() {
    baseRateService = new BaseRateService(rateTableRepository);
  }

  @Test
  void homFourFactorsExactPremium() {
    stubRate("HOM", "TX", "1000.0000", factor("AGE", "1.1000"), factor("TERR", "1.0500"),
        factor("CLAIMS", "0.9500"), factor("RISK-TIER", "1.0200"));
    var result =
        baseRateService.computeBasePremium("HOM", "COV1", "TX", LocalDate.parse("2026-01-15"));
    assertThat(result.ratingOutcome()).isEqualTo(RatingOutcome.ACCEPT);
    assertThat(result.compositeFactor()).isEqualByComparingTo("1.1192");
    assertThat(result.basePremium()).isEqualByComparingTo("1119.20");
  }

  @Test
  void autThreeFactorsExactPremium() {
    stubRate("AUT", "CA", "800.0000", factor("AGE", "1.2000"), factor("TERR", "1.0000"),
        factor("CLAIMS", "1.0000"));
    var result =
        baseRateService.computeBasePremium("AUT", "LIAB", "CA", LocalDate.parse("2026-01-15"));
    assertThat(result.basePremium()).isEqualByComparingTo("960.00");
  }

  @Test
  void cmlFiveFactorsExactPremium() {
    stubRate("CML", "NY", "5000.0000", factor("AGE", "1.0000"), factor("TERR", "1.1000"),
        factor("CLAIMS", "0.9000"), factor("RISK-TIER", "1.0000"), factor("OCCUPANCY", "1.0500"));
    var result =
        baseRateService.computeBasePremium("CML", "PROP", "NY", LocalDate.parse("2026-01-15"));
    assertThat(result.basePremium()).isEqualByComparingTo("5197.50");
  }

  @Test
  void missingFactorDefaultsToNeutralAndRecordsType() {
    stubRate("HOM", "TX", "1000.0000", factor("AGE", "1.1000"));
    var result =
        baseRateService.computeBasePremium("HOM", "COV1", "TX", LocalDate.parse("2026-01-15"));
    assertThat(result.missingFactorTypes()).contains("TERR", "CLAIMS", "RISK-TIER");
    assertThat(result.basePremium()).isEqualByComparingTo("1100.00");
  }

  @Test
  void rateNotFoundReturnsOutcome90() {
    when(rateTableRepository.findEffectiveRateTable("HOM", "TX", LocalDate.parse("2026-01-15")))
        .thenReturn(java.util.Optional.empty());
    var result =
        baseRateService.computeBasePremium("HOM", "COV1", "TX", LocalDate.parse("2026-01-15"));
    assertThat(result.ratingOutcome()).isEqualTo(RatingOutcome.RATE_NOT_FOUND);
    assertThat(result.basePremium()).isNull();
  }

  @Test
  void homeAliasMatchesHomExpectedFactors() {
    stubRate("HOME", "TX", "1200.0000", factor("OCCUPANCY", "1.0500"));
    var result =
        baseRateService.computeBasePremium("HOME", "COV1", "TX", LocalDate.parse("2026-01-15"));
    assertThat(result.missingFactorTypes()).contains("AGE", "TERR", "CLAIMS", "RISK-TIER");
    assertThat(result.basePremium()).isEqualByComparingTo("1200.00");
  }

  @Test
  void zeroBaseRateStillComputes() {
    stubRate("HOM", "TX", "0.0000", factor("AGE", "1.0000"), factor("TERR", "1.0000"),
        factor("CLAIMS", "1.0000"), factor("RISK-TIER", "1.0000"));
    var result =
        baseRateService.computeBasePremium("HOM", "COV1", "TX", LocalDate.parse("2026-01-15"));
    assertThat(result.basePremium()).isEqualByComparingTo("0.00");
  }

  @Test
  void rejectsBlankPolicyType() {
    assertThatThrownBy(
            () ->
                baseRateService.computeBasePremium(
                    " ", "COV1", "TX", LocalDate.parse("2026-01-15")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankTerritory() {
    assertThatThrownBy(
            () ->
                baseRateService.computeBasePremium(
                    "HOM", "COV1", " ", LocalDate.parse("2026-01-15")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullEffectiveDate() {
    assertThatThrownBy(() -> baseRateService.computeBasePremium("HOM", "COV1", "TX", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compositeFactorUsesScaleFour() {
    stubRate("HOM", "TX", "100.0000", factor("AGE", "1.3333"), factor("TERR", "1.0000"),
        factor("CLAIMS", "1.0000"), factor("RISK-TIER", "1.0000"));
    var result =
        baseRateService.computeBasePremium("HOM", "COV1", "TX", LocalDate.parse("2026-01-15"));
    assertThat(result.compositeFactor().scale()).isEqualTo(4);
    assertThat(result.basePremium().scale()).isEqualTo(2);
  }

  private void stubRate(String policyType, String territory, String baseRate, RateFactorRow... factors) {
    RateTableRow row =
        new RateTableRow(1L, policyType, territory, new BigDecimal(baseRate), LocalDate.parse("2025-01-01"));
    when(rateTableRepository.findEffectiveRateTable(policyType, territory, LocalDate.parse("2026-01-15")))
        .thenReturn(java.util.Optional.of(row));
    when(rateTableRepository.loadFactorsForRateTable(1L)).thenReturn(List.of(factors));
  }

  private static RateFactorRow factor(String code, String value) {
    return new RateFactorRow(1L, 1L, code, new BigDecimal(value));
  }
}
