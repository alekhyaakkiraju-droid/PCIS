package com.pcis.premium.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RatingRequest(
    String policyType,
    String coverageType,
    String territoryCode,
    String stateCode,
    LocalDate effectiveDate,
    BigDecimal coverageLimit,
    BigDecimal oldPremium,
    String policyNumber,
    String billingFrequencyCode,
    CustomerRiskData customerRiskData,
    EligibilityData eligibilityData) {

  public record CustomerRiskData(Integer age, Integer claimsCount, Integer creditScore) {}

  public record EligibilityData(Boolean multiPolicyDiscount, Integer claimsFreeYears) {}
}
