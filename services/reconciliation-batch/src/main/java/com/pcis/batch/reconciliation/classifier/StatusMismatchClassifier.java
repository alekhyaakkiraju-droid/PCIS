package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import org.springframework.stereotype.Component;

@Component
public class StatusMismatchClassifier extends BreakClassifier {

  public StatusMismatchClassifier() {
    super(BreakClass.STATUS_MISMATCH);
  }
}
