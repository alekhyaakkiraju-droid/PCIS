package com.pcis.billing.batch.prm005b.domain;

import java.util.Optional;

public record StatusTransition(
    String oldStatus,
    String newStatus,
    long daysPastDue,
    boolean incrementDelinquencyCounter) {}
