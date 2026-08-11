package com.pcis.premium.service;

import com.pcis.premium.config.PremiumRatingProperties;
import com.pcis.premium.infrastructure.SurchargeRuleRepository;
import com.pcis.premium.infrastructure.SurchargeRuleRepository.SurchargeRuleRow;
import com.pcis.premium.model.PremiumDetailLine;
import com.pcis.premium.model.PremiumDetailLine.DetailLineType;
import com.pcis.premium.model.RatingRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SurchargeService {

  private static final int MONEY_SCALE = 2;
  private static final int FACTOR_SCALE = 4;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private final SurchargeRuleRepository surchargeRuleRepository;
  private final PremiumRatingProperties properties;

  public SurchargeService(
      SurchargeRuleRepository surchargeRuleRepository, PremiumRatingProperties properties) {
    this.surchargeRuleRepository = surchargeRuleRepository;
    this.properties = properties;
  }

  public SurchargeResult applySurcharges(
      BigDecimal premiumAfterDiscounts, RatingRequest request, LocalDate effectiveDate) {
    String policyType = normalizePolicyType(request.policyType());
    List<SurchargeRuleRow> rules =
        surchargeRuleRepository.findEffectiveRules(policyType, effectiveDate);
    if (rules.isEmpty()) {
      return new SurchargeResult(premiumAfterDiscounts, List.of());
    }

    List<SurchargeRuleRow> mandatory = new ArrayList<>();
    List<SurchargeRuleRow> discretionary = new ArrayList<>();
    for (SurchargeRuleRow rule : rules) {
      if ("DISCRETIONARY".equalsIgnoreCase(rule.surchargeType())) {
        discretionary.add(rule);
      } else {
        mandatory.add(rule);
      }
    }

    BigDecimal running = premiumAfterDiscounts;
    List<PremiumDetailLine> lines = new ArrayList<>();

    running = applyMultiplicative(mandatory, running, lines, false);
    running =
        applyMultiplicative(
            discretionary, running, lines, true, properties.getMaxCombinedSurchargePct());
    running = applyFlat(rules, running, lines);

    return new SurchargeResult(running.setScale(MONEY_SCALE, ROUNDING), List.copyOf(lines));
  }

  private static BigDecimal applyMultiplicative(
      List<SurchargeRuleRow> rules,
      BigDecimal base,
      List<PremiumDetailLine> lines,
      boolean capCombined) {
    return applyMultiplicative(rules, base, lines, capCombined, null);
  }

  private static BigDecimal applyMultiplicative(
      List<SurchargeRuleRow> rules,
      BigDecimal base,
      List<PremiumDetailLine> lines,
      boolean capCombined,
      BigDecimal maxCombinedPct) {
    BigDecimal combinedFactor = BigDecimal.ONE;
    List<SurchargeRuleRow> multiplicative = new ArrayList<>();
    for (SurchargeRuleRow rule : rules) {
      if (!"MULTIPLICATIVE".equalsIgnoreCase(rule.calcType())) {
        continue;
      }
      multiplicative.add(rule);
      combinedFactor = combinedFactor.multiply(BigDecimal.ONE.add(rule.pct())).setScale(FACTOR_SCALE, ROUNDING);
    }
    if (capCombined && maxCombinedPct != null) {
      BigDecimal combinedPct = combinedFactor.subtract(BigDecimal.ONE);
      if (combinedPct.compareTo(maxCombinedPct) > 0) {
        combinedFactor = BigDecimal.ONE.add(maxCombinedPct);
      }
    }
    BigDecimal result = base.multiply(combinedFactor).setScale(MONEY_SCALE, ROUNDING);
    for (SurchargeRuleRow rule : multiplicative) {
      BigDecimal factor = rule.pct().setScale(FACTOR_SCALE, ROUNDING);
      BigDecimal amount = base.multiply(factor).setScale(MONEY_SCALE, ROUNDING);
      lines.add(new PremiumDetailLine(DetailLineType.SURCHARGE, rule.code(), rule.code(), factor, amount));
    }
    return result;
  }

  private static BigDecimal applyFlat(
      List<SurchargeRuleRow> rules, BigDecimal base, List<PremiumDetailLine> lines) {
    BigDecimal running = base;
    for (SurchargeRuleRow rule : rules) {
      if (!"FLAT".equalsIgnoreCase(rule.calcType())) {
        continue;
      }
      BigDecimal flat =
          rule.flatAmount() == null ? BigDecimal.ZERO : rule.flatAmount().setScale(MONEY_SCALE, ROUNDING);
      running = running.add(flat);
      lines.add(new PremiumDetailLine(DetailLineType.SURCHARGE, rule.code(), rule.code(), BigDecimal.ONE, flat));
    }
    return running;
  }

  private static String normalizePolicyType(String policyType) {
    if (policyType == null) {
      return "";
    }
    String normalized = policyType.trim().toUpperCase();
    return "HOME".equals(normalized) ? "HOM" : normalized;
  }

  public record SurchargeResult(BigDecimal premiumAfterSurcharges, List<PremiumDetailLine> lines) {}
}
