package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import org.springframework.stereotype.Component;

@Component
public class ChecksumMismatchClassifier extends BreakClassifier {

  public ChecksumMismatchClassifier() {
    super(BreakClass.CHECKSUM_MISMATCH);
  }
}
