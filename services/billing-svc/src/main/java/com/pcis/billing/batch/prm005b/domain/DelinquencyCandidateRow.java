package com.pcis.billing.batch.prm005b.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DelinquencyCandidateRow(
    long billSchedId,
    String polNbr,
    int installmentNbr,
    LocalDate dueDate,
    BigDecimal amtDue,
    BigDecimal amtPaid,
    String schedStatus,
    int recDelinquent,
    long daysPastDue,
    long version) {}
