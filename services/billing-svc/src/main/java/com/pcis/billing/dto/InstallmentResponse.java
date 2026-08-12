package com.pcis.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
    String id, String policyId, LocalDate dueDate, BigDecimal amount, String status) {}
