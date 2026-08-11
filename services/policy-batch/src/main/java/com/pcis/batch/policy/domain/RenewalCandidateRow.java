package com.pcis.batch.policy.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Active policy row selected for renewal within the configured window. */
public record RenewalCandidateRow(
    String polNbr,
    String custId,
    String agtId,
    String policyType,
    LocalDate effDate,
    LocalDate expDate,
    BigDecimal premAnnual,
    String billFreq,
    String stateCode) {}
