package com.pcis.premium.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pcis.premium.config.PremiumRatingProperties;
import com.pcis.premium.domain.RateLookupNotFoundException;
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
class PremiumRatingServiceTest {

  @Mock private RateTableRepository rateTableRepository;

  private PremiumRatingService service;

  @BeforeEach
  void setUp() {
    PremiumRatingProperties properties = new PremiumRatingProperties();
    properties.setDecimalScale(2);
    service = new PremiumRatingService(rateTableRepository, properties);
  }

  @Test
  void lookupBaseRateAndFactorsMultipliesAndRoundsToConfiguredScale() {
    var rateTable =
        new RateTableRow(1L, "HOME", "TX", new BigDecimal("1200.00"), LocalDate.now());
    var factors =
        List.of(new RateFactorRow(10L, 1L, "OCCUPANCY", new BigDecimal("1.0500")));

    when(rateTableRepository.findEffectiveRateTable("HOME", "TX")).thenReturn(java.util.Optional.of(rateTable));
    when(rateTableRepository.loadFactorsForRateTable(1L)).thenReturn(factors);

    var result = service.lookupBaseRateAndFactors("HOME", "TX");

    assertThat(result.baseRate()).isEqualByComparingTo("1200.00");
    assertThat(result.combinedFactor()).isEqualByComparingTo("1.0500");
    assertThat(result.basePremium()).isEqualByComparingTo("1260.00");
    assertThat(result.factors()).hasSize(1);
  }

  @Test
  void lookupBaseRateAndFactorsCombinesMultipleFactors() {
    var rateTable =
        new RateTableRow(2L, "HOME", "TX", new BigDecimal("1000.00"), LocalDate.now());
    var factors =
        List.of(
            new RateFactorRow(1L, 2L, "OCCUPANCY", new BigDecimal("1.0500")),
            new RateFactorRow(2L, 2L, "CONSTRUCTION", new BigDecimal("1.0200")));

    when(rateTableRepository.findEffectiveRateTable("HOME", "TX")).thenReturn(java.util.Optional.of(rateTable));
    when(rateTableRepository.loadFactorsForRateTable(2L)).thenReturn(factors);

    var result = service.lookupBaseRateAndFactors("HOME", "TX");

    assertThat(result.combinedFactor()).isEqualByComparingTo("1.0710");
    assertThat(result.basePremium()).isEqualByComparingTo("1071.00");
  }

  @Test
  void lookupBaseRateAndFactorsThrowsWhenRateTableMissing() {
    when(rateTableRepository.findEffectiveRateTable("AUTO", "CA"))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> service.lookupBaseRateAndFactors("AUTO", "CA"))
        .isInstanceOf(RateLookupNotFoundException.class)
        .hasMessageContaining("AUTO")
        .hasMessageContaining("CA");
  }

  @Test
  void lookupBaseRateAndFactorsRejectsBlankTerritory() {
    assertThatThrownBy(() -> service.lookupBaseRateAndFactors("HOME", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("territory");
  }
}
