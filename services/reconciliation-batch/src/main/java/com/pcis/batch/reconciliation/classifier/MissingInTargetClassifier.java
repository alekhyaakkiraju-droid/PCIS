package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import org.springframework.stereotype.Component;

@Component
public class MissingInTargetClassifier extends BreakClassifier {

  public MissingInTargetClassifier() {
    super(BreakClass.MISSING_IN_TARGET);
  }
}
