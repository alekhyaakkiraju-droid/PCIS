package com.pcis.claims.dto;

import java.math.BigDecimal;

public record PaymentResponse(
    Long paymentId,
    String claimNbr,
    BigDecimal paymentAmt,
    String paymentStatus,
    Long approvalId,
    Integer payeeId,
    String adjusterId) {}
