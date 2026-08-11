package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import org.springframework.stereotype.Component;

@Component
public class MissingInLegacyClassifier extends BreakClassifier {

  public MissingInLegacyClassifier() {
    super(BreakClass.MISSING_IN_LEGACY);
  }
}
