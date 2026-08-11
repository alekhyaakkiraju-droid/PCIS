package com.pcis.premium.service;

import com.pcis.premium.config.PremiumRatingProperties;
import com.pcis.premium.infrastructure.DiscountRuleRepository;
import com.pcis.premium.infrastructure.DiscountRuleRepository.DiscountRuleRow;
import com.pcis.premium.model.PremiumDetailLine;
import com.pcis.premium.model.PremiumDetailLine.DetailLineType;
import com.pcis.premium.model.RatingRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountService {

  private static final int MONEY_SCALE = 2;
  private static final int FACTOR_SCALE = 4;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private final DiscountRuleRepository discountRuleRepository;
  private final PremiumRatingProperties properties;

  public DiscountService(
      DiscountRuleRepository discountRuleRepository, PremiumRatingProperties properties) {
    this.discountRuleRepository = discountRuleRepository;
    this.properties = properties;
  }

  public DiscountResult applyDiscounts(
      BigDecimal basePremium, RatingRequest request, LocalDate effectiveDate) {
    String policyType = normalizePolicyType(request.policyType());
    List<DiscountRuleRow> rules =
        discountRuleRepository.findEffectiveRules(policyType, effectiveDate).stream()
            .filter(rule -> isEligible(rule, request))
            .toList();

    if (rules.isEmpty()) {
      return new DiscountResult(basePremium, List.of());
    }

    Map<String, DiscountRuleRow> bestByGroup = new LinkedHashMap<>();
    List<DiscountRuleRow> flatRules = new ArrayList<>();

    for (DiscountRuleRow rule : rules) {
      if ("FLAT".equalsIgnoreCase(rule.discountType())) {
        flatRules.add(rule);
        continue;
      }
      String group = rule.stackingGroup() == null ? rule.code() : rule.stackingGroup();
      bestByGroup.merge(
          group,
          rule,
          (existing, candidate) ->
              candidate.pct().compareTo(existing.pct()) > 0 ? candidate : existing);
    }

    BigDecimal retentionFactor = BigDecimal.ONE;
    List<PremiumDetailLine> lines = new ArrayList<>();
    for (DiscountRuleRow rule : bestByGroup.values()) {
      BigDecimal pct = rule.pct().setScale(FACTOR_SCALE, ROUNDING);
      retentionFactor = retentionFactor.multiply(BigDecimal.ONE.subtract(pct)).setScale(FACTOR_SCALE, ROUNDING);
      BigDecimal amount =
          basePremium.multiply(pct).setScale(MONEY_SCALE, ROUNDING).negate();
      lines.add(new PremiumDetailLine(DetailLineType.DISCOUNT, rule.code(), rule.code(), pct, amount));
    }

    BigDecimal totalDiscountPct = BigDecimal.ONE.subtract(retentionFactor);
    BigDecimal maxCombined = resolveMaxCombinedPct(rules);
    if (totalDiscountPct.compareTo(maxCombined) > 0) {
      retentionFactor = BigDecimal.ONE.subtract(maxCombined);
    }

    BigDecimal afterMultiplicative =
        basePremium.multiply(retentionFactor).setScale(MONEY_SCALE, ROUNDING);

    for (DiscountRuleRow flat : flatRules) {
      BigDecimal flatAmt =
          flat.flatAmount() == null ? BigDecimal.ZERO : flat.flatAmount().setScale(MONEY_SCALE, ROUNDING);
      afterMultiplicative = afterMultiplicative.subtract(flatAmt).max(BigDecimal.ZERO);
      lines.add(
          new PremiumDetailLine(
              DetailLineType.DISCOUNT,
              flat.code(),
              flat.code(),
              BigDecimal.ONE,
              flatAmt.negate()));
    }

    return new DiscountResult(afterMultiplicative.setScale(MONEY_SCALE, ROUNDING), List.copyOf(lines));
  }

  private BigDecimal resolveMaxCombinedPct(List<DiscountRuleRow> rules) {
    return rules.stream()
        .map(DiscountRuleRow::maxCombinedPct)
        .filter(pct -> pct != null)
        .max(Comparator.naturalOrder())
        .orElse(properties.getMaxCombinedDiscountPct());
  }

  private static boolean isEligible(DiscountRuleRow rule, RatingRequest request) {
    if (rule.eligibilityCode() == null || rule.eligibilityCode().isBlank()) {
      return true;
    }
    if (request.eligibilityData() == null) {
      return false;
    }
    return switch (rule.eligibilityCode().toUpperCase()) {
      case "MULTI_POLICY" ->
          Boolean.TRUE.equals(request.eligibilityData().multiPolicyDiscount());
      case "CLAIMS_FREE" ->
          request.eligibilityData().claimsFreeYears() != null
              && request.eligibilityData().claimsFreeYears() >= 3;
      default -> true;
    };
  }

  private static String normalizePolicyType(String policyType) {
    if (policyType == null) {
      return "";
    }
    String normalized = policyType.trim().toUpperCase();
    return "HOME".equals(normalized) ? "HOM" : normalized;
  }

  public record DiscountResult(BigDecimal premiumAfterDiscounts, List<PremiumDetailLine> lines) {}
}
