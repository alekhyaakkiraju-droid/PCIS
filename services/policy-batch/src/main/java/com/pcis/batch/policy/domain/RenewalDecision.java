package com.pcis.batch.policy.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Processor output describing the renewal term to persist. */
public record RenewalDecision(
    RenewalCandidateRow source,
    String newPolNbr,
    LocalDate newEffDate,
    LocalDate newExpDate,
    BigDecimal newPremium,
    boolean referralFlag) {}
