package com.pcis.premium.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.dto.CreateCalculationRequest;
import com.pcis.premium.infrastructure.PremiumCalcRepository;
import com.pcis.premium.model.RatingResponse;
import com.pcis.premium.model.UnderwritingDecision;
import com.pcis.premium.service.RatingPipelineOrchestrator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PremiumRatingServiceTest {

  @Mock private RatingPipelineOrchestrator orchestrator;
  @Mock private PremiumCalcRepository premiumCalcRepository;

  private PremiumRatingService service;

  @BeforeEach
  void setUp() {
    service = new PremiumRatingService(orchestrator, premiumCalcRepository);
  }

  @Test
  void createCalculationMapsOrchestratorResponse() {
    when(orchestrator.orchestrate(any()))
        .thenReturn(
            new RatingResponse(
                "calc-1",
                RatingOutcome.ACCEPT,
                UnderwritingDecision.APPROVE,
                "00",
                new BigDecimal("35.0000"),
                "B",
                new BigDecimal("1200.00"),
                new BigDecimal("1.0500"),
                new BigDecimal("1260.00"),
                new BigDecimal("1260.00"),
                new BigDecimal("1260.00"),
                new BigDecimal("1323.00"),
                null,
                null,
                List.of(),
                List.of(new BigDecimal("1323.00")),
                false));

    var response =
        service.createCalculation(new CreateCalculationRequest("HOME", "TX", null, null, null, null, null, null));

    assertThat(response.calculationId()).isEqualTo("calc-1");
    assertThat(response.returnCode()).isEqualTo("00");
    assertThat(response.finalPremium()).isEqualByComparingTo("1323.00");
  }

  @Test
  void getCalculationLoadsStoredSnapshot() {
    when(premiumCalcRepository.findBySnapshotId("calc-1"))
        .thenReturn(
            Optional.of(
                new PremiumCalcRepository.StoredCalculation(
                    1L, "HO123", new BigDecimal("1323.00"), null, "calc-1")));

    var response = service.getCalculation("calc-1");
    assertThat(response.calculationId()).isEqualTo("calc-1");
    assertThat(response.finalPremium()).isEqualByComparingTo("1323.00");
  }

  @Test
  void getCalculationRejectsBlankId() {
    assertThatThrownBy(() -> service.ensureReadPathWired(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
