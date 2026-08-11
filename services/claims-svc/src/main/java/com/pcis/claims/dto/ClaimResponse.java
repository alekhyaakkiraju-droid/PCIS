package com.pcis.claims.dto;

import java.time.LocalDate;

public record ClaimResponse(
    String claimNbr,
    String polNbr,
    Integer custId,
    LocalDate lossDate,
    String claimType,
    String claimStatus) {}
