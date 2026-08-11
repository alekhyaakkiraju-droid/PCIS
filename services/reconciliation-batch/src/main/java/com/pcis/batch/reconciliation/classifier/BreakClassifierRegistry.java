package com.pcis.batch.reconciliation.classifier;

import com.pcis.batch.reconciliation.domain.BreakClass;
import com.pcis.batch.reconciliation.domain.ReconciliationBreakRecord;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BreakClassifierRegistry {

  private final Map<BreakClass, BreakClassifier> classifiers = new EnumMap<>(BreakClass.class);

  public BreakClassifierRegistry(
      MissingInTargetClassifier missingInTargetClassifier,
      MissingInLegacyClassifier missingInLegacyClassifier,
      ValueMismatchClassifier valueMismatchClassifier,
      CountMismatchClassifier countMismatchClassifier,
      ChecksumMismatchClassifier checksumMismatchClassifier,
      StatusMismatchClassifier statusMismatchClassifier) {
    classifiers.put(BreakClass.MISSING_IN_TARGET, missingInTargetClassifier);
    classifiers.put(BreakClass.MISSING_IN_LEGACY, missingInLegacyClassifier);
    classifiers.put(BreakClass.VALUE_MISMATCH, valueMismatchClassifier);
    classifiers.put(BreakClass.COUNT_MISMATCH, countMismatchClassifier);
    classifiers.put(BreakClass.CHECKSUM_MISMATCH, checksumMismatchClassifier);
    classifiers.put(BreakClass.STATUS_MISMATCH, statusMismatchClassifier);
  }

  public ReconciliationBreakRecord classify(
      BreakClass breakClass,
      long runId,
      String domain,
      String entityName,
      String businessKey,
      String columnName,
      String legacyValue,
      String targetValue,
      String approvedDecisionId) {
    BreakClassifier classifier = classifiers.get(breakClass);
    if (classifier == null) {
      throw new IllegalArgumentException("Unknown break class: " + breakClass);
    }
    return classifier.classify(
        runId,
        domain,
        entityName,
        businessKey,
        columnName,
        legacyValue,
        targetValue,
        approvedDecisionId);
  }
}
