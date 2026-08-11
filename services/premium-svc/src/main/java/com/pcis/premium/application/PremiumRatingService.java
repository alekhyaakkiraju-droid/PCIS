package com.pcis.premium.application;

import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.dto.CreateCalculationRequest;
import com.pcis.premium.dto.PremiumCalculationResponse;
import com.pcis.premium.dto.PremiumCalculationResponse.RatingComponentLine;
import com.pcis.premium.infrastructure.PremiumCalcRepository;
import com.pcis.premium.model.PremiumDetailLine;
import com.pcis.premium.model.PremiumDetailLine.DetailLineType;
import com.pcis.premium.model.RatingRequest;
import com.pcis.premium.model.RatingResponse;
import com.pcis.premium.service.RatingPipelineOrchestrator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PremiumRatingService {

  private final RatingPipelineOrchestrator orchestrator;
  private final PremiumCalcRepository premiumCalcRepository;

  public PremiumRatingService(
      RatingPipelineOrchestrator orchestrator, PremiumCalcRepository premiumCalcRepository) {
    this.orchestrator = orchestrator;
    this.premiumCalcRepository = premiumCalcRepository;
  }

  public PremiumCalculationResponse createCalculation(CreateCalculationRequest request) {
    RatingResponse response = orchestrator.orchestrate(toRatingRequest(request));
    return toApiResponse(response);
  }

  public PremiumCalculationResponse getCalculation(String calculationId) {
    ensureReadPathWired(calculationId);
    return premiumCalcRepository
        .findBySnapshotId(calculationId)
        .map(
            stored ->
                new PremiumCalculationResponse(
                    stored.snapshotId(),
                    RatingOutcome.ACCEPT.returnCode(),
                    "APPROVE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    stored.finalPremium(),
                    null,
                    null,
                    List.of()))
        .orElseThrow(() -> new CalculationNotFoundException(calculationId));
  }

  public void ensureReadPathWired(String calculationId) {
    if (calculationId == null || calculationId.isBlank()) {
      throw new IllegalArgumentException("calculationId is required");
    }
  }

  private static RatingRequest toRatingRequest(CreateCalculationRequest request) {
    return new RatingRequest(
        request.policyType(),
        request.coverageType(),
        request.territory(),
        request.state(),
        LocalDate.now(),
        parseDecimal(request.limit()),
        parseDecimal(request.oldPremium()),
        request.policyNumber(),
        request.billingFrequency(),
        null,
        null);
  }

  private static BigDecimal parseDecimal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new BigDecimal(value.trim());
  }

  private static PremiumCalculationResponse toApiResponse(RatingResponse response) {
    List<RatingComponentLine> factors = new ArrayList<>();
    List<RatingComponentLine> discounts = new ArrayList<>();
    List<RatingComponentLine> surcharges = new ArrayList<>();
    List<RatingComponentLine> taxes = new ArrayList<>();

    for (PremiumDetailLine line : response.detailLines()) {
      RatingComponentLine component =
          new RatingComponentLine(line.code(), line.factor(), line.amount());
      if (line.lineType() == DetailLineType.DISCOUNT) {
        discounts.add(component);
      } else if (line.lineType() == DetailLineType.SURCHARGE) {
        surcharges.add(component);
      } else if (line.lineType() == DetailLineType.TAX) {
        taxes.add(component);
      } else {
        factors.add(component);
      }
    }

    List<String> installmentStrings =
        response.installmentAmounts().stream().map(BigDecimal::toPlainString).toList();

    return new PremiumCalculationResponse(
        response.calculationId(),
        response.returnCode(),
        response.underwritingDecision().name(),
        response.compositeRiskScore(),
        response.riskTier(),
        response.baseRate(),
        response.ratingFactor(),
        response.basePremium(),
        factors,
        discounts,
        surcharges,
        taxes,
        response.finalPremium(),
        response.matchedRuleId(),
        response.matchedRuleText(),
        installmentStrings);
  }

  public static final class CalculationNotFoundException extends RuntimeException {
    public CalculationNotFoundException(String calculationId) {
      super("Calculation not found: " + calculationId);
    }
  }
}
