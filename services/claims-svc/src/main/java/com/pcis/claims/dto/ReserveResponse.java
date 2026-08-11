package com.pcis.claims.dto;

import java.math.BigDecimal;

public record ReserveResponse(
    Long reserveId,
    String claimNbr,
    String reserveType,
    BigDecimal approvedAmt,
    BigDecimal paidToDate,
    String reserveStatus) {}
