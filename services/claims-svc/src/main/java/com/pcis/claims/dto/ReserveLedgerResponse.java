package com.pcis.claims.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReserveLedgerResponse(
    Long ledgerId,
    String claimNbr,
    Long reserveId,
    LocalDate eventDate,
    String reason,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String actorId,
    String eventType) {}
