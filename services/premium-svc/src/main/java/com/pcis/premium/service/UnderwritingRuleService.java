package com.pcis.premium.service;

import com.pcis.premium.domain.RatingOutcome;
import com.pcis.premium.infrastructure.UwRuleRepository;
import com.pcis.premium.infrastructure.UwRuleRepository.UwRuleRow;
import com.pcis.premium.model.RatingRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class UnderwritingRuleService {

  private final UwRuleRepository uwRuleRepository;

  public UnderwritingRuleService(UwRuleRepository uwRuleRepository) {
    this.uwRuleRepository = uwRuleRepository;
  }

  public UwEvaluationResult evaluate(RatingRequest request) {
    String policyType = normalizePolicyType(request.policyType());
    for (UwRuleRow rule : uwRuleRepository.findRulesForPolicyType(policyType)) {
      if (!matches(rule, request)) {
        continue;
      }
      RatingOutcome outcome = mapOutcome(rule.outcome());
      if (outcome == RatingOutcome.DECLINE || outcome == RatingOutcome.REFERRAL) {
        return new UwEvaluationResult(outcome, rule.ruleId(), rule.ruleText());
      }
    }
    return UwEvaluationResult.accept();
  }

  private static boolean matches(UwRuleRow rule, RatingRequest request) {
    if (rule.conditionField() == null || rule.conditionValue() == null) {
      return false;
    }
    BigDecimal actual = resolveField(rule.conditionField(), request);
    if (actual == null) {
      return false;
    }
    return switch (rule.conditionOperator()) {
      case "GT", ">" -> actual.compareTo(rule.conditionValue()) > 0;
      case "GTE", ">=" -> actual.compareTo(rule.conditionValue()) >= 0;
      case "LT", "<" -> actual.compareTo(rule.conditionValue()) < 0;
      case "LTE", "<=" -> actual.compareTo(rule.conditionValue()) <= 0;
      case "EQ", "=" -> actual.compareTo(rule.conditionValue()) == 0;
      default -> false;
    };
  }

  private static BigDecimal resolveField(String field, RatingRequest request) {
    return switch (field.toUpperCase()) {
      case "LIMIT", "COVERAGE_LIMIT" -> request.coverageLimit();
      case "OLD_PREMIUM" -> request.oldPremium();
      case "CLAIMS_COUNT" ->
          request.customerRiskData() != null && request.customerRiskData().claimsCount() != null
              ? BigDecimal.valueOf(request.customerRiskData().claimsCount())
              : null;
      default -> null;
    };
  }

  private static RatingOutcome mapOutcome(String outcome) {
    if (outcome == null) {
      return RatingOutcome.ACCEPT;
    }
    return switch (outcome.toUpperCase()) {
      case "DECLINE" -> RatingOutcome.DECLINE;
      case "REFER" -> RatingOutcome.REFERRAL;
      default -> RatingOutcome.ACCEPT;
    };
  }

  private static String normalizePolicyType(String policyType) {
    if (policyType == null) {
      return "";
    }
    String normalized = policyType.trim().toUpperCase();
    return "HOME".equals(normalized) ? "HOM" : normalized;
  }

  public record UwEvaluationResult(
      RatingOutcome outcome, Long matchedRuleId, String matchedRuleText) {

    static UwEvaluationResult accept() {
      return new UwEvaluationResult(RatingOutcome.ACCEPT, null, null);
    }
  }
}
