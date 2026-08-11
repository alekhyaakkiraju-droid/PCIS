package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import org.springframework.stereotype.Component;

@Component
public class ValueMismatchClassifier extends BreakClassifier {

  public ValueMismatchClassifier() {
    super(BreakClass.VALUE_MISMATCH);
  }
}
