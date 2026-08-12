package com.pcis.claims.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimListItemResponse(
    String claimNbr,
    String polNbr,
    Integer custId,
    LocalDate lossDate,
    String claimType,
    String claimStatus,
    BigDecimal reserveRemaining,
    BigDecimal totalApprovedAmt,
    BigDecimal totalPaidToDate,
    String adjusterId,
    String adjusterName,
    boolean pendingApproval) {}
