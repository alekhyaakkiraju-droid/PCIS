package com.pcis.batch.reconciliation.comparator;

import com.pcis.batch.reconciliation.domain.DomainComparisonResult;
import java.time.LocalDate;

public interface DomainComparator {

  String domain();

  DomainComparisonResult compare(long runId, LocalDate businessDate);
}
