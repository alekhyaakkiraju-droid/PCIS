package com.pcis.premium.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.premium.domain.BaseRateResult;
import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.infrastructure.PremiumCalcRepository;
import com.pcis.premium.model.BillingFrequency;
import com.pcis.premium.model.PremiumDetailLine;
import com.pcis.premium.model.PremiumDetailLine.DetailLineType;
import com.pcis.premium.model.RatingRequest;
import com.pcis.premium.model.RatingResponse;
import com.pcis.premium.model.UnderwritingDecision;
import com.pcis.premium.config.PremiumRatingProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingPipelineOrchestrator {

  private static final String CRT_USER = "PRMCLC01";

  private final BaseRateService baseRateService;
  private final RiskScoreService riskScoreService;
  private final UnderwritingRuleService underwritingRuleService;
  private final DiscountService discountService;
  private final SurchargeService surchargeService;
  private final TaxService taxService;
  private final InstallmentDivisionService installmentDivisionService;
  private final PremiumCalcRepository premiumCalcRepository;
  private final OutboxEventWriter ratingOutboxEventWriter;
  private final PremiumRatingProperties properties;

  public RatingPipelineOrchestrator(
      BaseRateService baseRateService,
      RiskScoreService riskScoreService,
      UnderwritingRuleService underwritingRuleService,
      DiscountService discountService,
      SurchargeService surchargeService,
      TaxService taxService,
      InstallmentDivisionService installmentDivisionService,
      PremiumCalcRepository premiumCalcRepository,
      @Qualifier("ratingOutboxEventWriter") OutboxEventWriter ratingOutboxEventWriter,
      PremiumRatingProperties properties) {
    this.baseRateService = baseRateService;
    this.riskScoreService = riskScoreService;
    this.underwritingRuleService = underwritingRuleService;
    this.discountService = discountService;
    this.surchargeService = surchargeService;
    this.taxService = taxService;
    this.installmentDivisionService = installmentDivisionService;
    this.premiumCalcRepository = premiumCalcRepository;
    this.ratingOutboxEventWriter = ratingOutboxEventWriter;
    this.properties = properties;
  }

  @Transactional
  public RatingResponse orchestrate(RatingRequest request) {
    validateRequest(request);
    LocalDate effectiveDate =
        request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate();
    String calculationId = UUID.randomUUID().toString();
    String territory = resolveTerritory(request);

    var risk = riskScoreService.computeCompositeRiskScore(request);
    var uw = underwritingRuleService.evaluate(request);

    if (uw.outcome() == RatingOutcome.DECLINE) {
      writeAuditEvent(calculationId, request, uw.outcome(), null);
      return RatingResponse.decline(
          calculationId,
          uw.matchedRuleId(),
          uw.matchedRuleText(),
          risk.compositeScore(),
          risk.riskTier());
    }

    BaseRateResult baseRate =
        baseRateService.computeBasePremium(
            request.policyType(), request.coverageType(), territory, effectiveDate);
    if (baseRate.ratingOutcome() == RatingOutcome.RATE_NOT_FOUND) {
      return buildRateNotFound(calculationId, risk, uw.outcome());
    }

    List<PremiumDetailLine> detailLines = new ArrayList<>();
    detailLines.add(
        new PremiumDetailLine(
            DetailLineType.FACTOR,
            "BASE",
            "Base premium",
            baseRate.compositeFactor(),
            baseRate.basePremium()));

    var discount = discountService.applyDiscounts(baseRate.basePremium(), request, effectiveDate);
    detailLines.addAll(discount.lines());

    var surcharge =
        surchargeService.applySurcharges(discount.premiumAfterDiscounts(), request, effectiveDate);
    detailLines.addAll(surcharge.lines());

    var tax =
        taxService.calculateTaxes(
            surcharge.premiumAfterSurcharges(), request.stateCode(), effectiveDate);
    detailLines.addAll(tax.lines());

    BigDecimal finalPremium = tax.finalPremium();
    BillingFrequency billingFrequency =
        BillingFrequency.fromCode(
            request.billingFrequencyCode() == null ? "A" : request.billingFrequencyCode());
    List<BigDecimal> installments =
        installmentDivisionService.divideAnnualPremium(
            finalPremium, billingFrequency.getInstallmentCount());

    boolean reinsuranceFlag =
        finalPremium.compareTo(properties.getReinsuranceCessionThreshold()) > 0;

    long calcId =
        premiumCalcRepository.insertCalculation(
            request.policyNumber(), finalPremium, calculationId, CRT_USER);
    premiumCalcRepository.insertDetailLines(calcId, detailLines, CRT_USER);
    writeAuditEvent(calculationId, request, uw.outcome(), finalPremium);

    return new RatingResponse(
        calculationId,
        uw.outcome(),
        UnderwritingDecision.fromOutcome(uw.outcome()),
        uw.outcome().returnCode(),
        risk.compositeScore(),
        risk.riskTier(),
        baseRate.baseRate(),
        baseRate.compositeFactor(),
        baseRate.basePremium(),
        discount.premiumAfterDiscounts(),
        surcharge.premiumAfterSurcharges(),
        finalPremium,
        uw.matchedRuleId(),
        uw.matchedRuleText(),
        List.copyOf(detailLines),
        installments,
        reinsuranceFlag);
  }

  private static void validateRequest(RatingRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (request.policyType() == null || request.policyType().isBlank()) {
      throw new IllegalArgumentException("policyType is required");
    }
    if (request.stateCode() == null || request.stateCode().isBlank()) {
      throw new IllegalArgumentException("stateCode is required");
    }
  }

  private static String resolveTerritory(RatingRequest request) {
    if (request.territoryCode() != null && !request.territoryCode().isBlank()) {
      return request.territoryCode().trim();
    }
    return request.stateCode().trim();
  }

  private RatingResponse buildRateNotFound(
      String calculationId,
      RiskScoreService.RiskScoreResult risk,
      RatingOutcome uwOutcome) {
    return new RatingResponse(
        calculationId,
        RatingOutcome.RATE_NOT_FOUND,
        UnderwritingDecision.APPROVE,
        RatingOutcome.RATE_NOT_FOUND.returnCode(),
        risk.compositeScore(),
        risk.riskTier(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        List.of(),
        false);
  }

  private void writeAuditEvent(
      String calculationId,
      RatingRequest request,
      RatingOutcome outcome,
      BigDecimal finalPremium) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("calculationId", calculationId);
    payload.put("policyNumber", request.policyNumber());
    payload.put("policyType", request.policyType());
    payload.put("returnCode", outcome.returnCode());
    payload.put("finalPremium", finalPremium);
    ratingOutboxEventWriter.write(
        "PREMIUM_CALC",
        calculationId,
        "PremiumRated",
        payload,
        UUID.fromString(calculationId));
  }
}
