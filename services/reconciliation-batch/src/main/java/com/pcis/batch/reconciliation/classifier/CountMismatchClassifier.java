package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import org.springframework.stereotype.Component;

@Component
public class CountMismatchClassifier extends BreakClassifier {

  public CountMismatchClassifier() {
    super(BreakClass.COUNT_MISMATCH);
  }
}
