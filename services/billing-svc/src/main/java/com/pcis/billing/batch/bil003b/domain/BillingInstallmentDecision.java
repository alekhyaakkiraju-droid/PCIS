package com.pcis.billing.batch.bil003b.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingInstallmentDecision(
    BillingCandidateRow candidate,
    int installmentNbr,
    LocalDate dueDate,
    BigDecimal amount) {}
