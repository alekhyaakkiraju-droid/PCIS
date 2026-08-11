package com.pcis.billing.batch.bil003b.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingCandidateRow(
    String polNbr,
    BigDecimal premAnnual,
    String billFreq,
    int installmentCnt,
    long billPlanId,
    int lastInstallmentNbr,
    LocalDate lastDueDate) {}
