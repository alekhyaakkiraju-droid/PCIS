package com.pcis.billing.batch.cmm001b.domain;

import java.math.BigDecimal;

public record CommissionDecision(
    CommissionCandidateRow candidate, BigDecimal commissionAmount, boolean hasPlan) {}
