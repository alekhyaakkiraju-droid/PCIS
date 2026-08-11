package com.pcis.policy.batch.pol006b.client;

import java.math.BigDecimal;

record RatingRequest(
    String policyType, String state, String oldPremium, String policyNumber) {}

record RatingResponsePayload(
    String calculationId,
    String returnCode,
    String underwritingDecision,
    BigDecimal finalPremium) {}
