package com.pcis.batch.policy.client;

import java.math.BigDecimal;

public record PremiumRatingRequest(
    String policyType, String state, String oldPremium, String policyNumber) {}
