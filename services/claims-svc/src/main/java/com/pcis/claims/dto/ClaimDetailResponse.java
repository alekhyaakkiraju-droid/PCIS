package com.pcis.claims.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ClaimDetailResponse(
    String claimNbr,
    String polNbr,
    Integer custId,
    LocalDate lossDate,
    String claimType,
    String claimStatus,
    Long version,
    BigDecimal authorityLimit,
    List<ReserveResponse> reserves,
    List<PaymentResponse> payments,
    List<NoteResponse> notes) {}
