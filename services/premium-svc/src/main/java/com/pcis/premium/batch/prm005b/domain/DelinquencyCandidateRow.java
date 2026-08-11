package com.pcis.premium.batch.prm005b.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DelinquencyCandidateRow(
    long billSchedId,
    String polNbr,
    LocalDate dueDate,
    BigDecimal amtDue,
    BigDecimal amtPaid,
    String schedStatus,
    int daysPastDue) {}
