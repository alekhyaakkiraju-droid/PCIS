package com.pcis.billing.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentResponse(
    String paymentId,
    String polNbr,
    BigDecimal paymentAmt,
    LocalDate paymentDate,
    String paymentStatus,
    List<PaymentAllocationDetail> allocations) {}
