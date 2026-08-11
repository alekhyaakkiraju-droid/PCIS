package com.pcis.masking;

import com.pcis.classification.ClassificationEntry;
import com.pcis.classification.DataClassificationRegistry;
import com.pcis.classification.MaskStrategy;
import com.pcis.masking.mask.MaskerRegistry;
import com.pcis.masking.mask.ValueMasker;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metadata-driven PII masking service keyed on entity plus column against the classification
 * registry.
 */
public class MaskingService {

  private static final Logger log = LoggerFactory.getLogger(MaskingService.class);

  private final DataClassificationRegistry registry;
  private final MaskerRegistry maskerRegistry;
  private final PolymorphicMaskResolver polymorphicMaskResolver;

  public MaskingService(DataClassificationRegistry registry) {
    this(registry, new MaskerRegistry(), new PolymorphicMaskResolver());
  }

  MaskingService(
      DataClassificationRegistry registry,
      MaskerRegistry maskerRegistry,
      PolymorphicMaskResolver polymorphicMaskResolver) {
    this.registry = registry;
    this.maskerRegistry = maskerRegistry;
    this.polymorphicMaskResolver = polymorphicMaskResolver;
  }

  /** Masks {@code value} using the registry entry for {@code entityName}.{columnName}. */
  public String mask(String entityName, String columnName, String value) {
    return mask(entityName, columnName, value, null);
  }

  /**
   * Masks {@code value} using the registry entry, optionally resolving a polymorphic strategy via
   * {@code discriminatorValue}.
   */
  public String mask(String entityName, String columnName, String value, String discriminatorValue) {
    if (value == null) {
      return null;
    }
    try {
      Optional<ClassificationEntry> entry = registry.findEntry(entityName, columnName);
      if (entry.isEmpty()) {
        log.warn(
            "No classification for {}.{} — failing closed to FULL_REDACT",
            entityName,
            columnName);
        return maskByClassification(value, MaskStrategy.FULL_REDACT);
      }
      MaskStrategy strategy =
          polymorphicMaskResolver.resolve(entry.get(), discriminatorValue).orElse(entry.get().maskStrategy());
      return maskByClassification(value, strategy);
    } catch (RuntimeException ex) {
      log.warn(
          "Masking failed for {}.{} — degrading to FULL_REDACT: {}",
          entityName,
          columnName,
          ex.toString());
      return maskByClassification(value, MaskStrategy.FULL_REDACT);
    }
  }

  /** Applies a {@link MaskStrategy} directly without registry lookup. */
  public String maskByClassification(String value, MaskStrategy strategy) {
    if (value == null) {
      return null;
    }
    if (strategy == null) {
      strategy = MaskStrategy.FULL_REDACT;
    }
    try {
      ValueMasker masker = maskerRegistry.get(strategy);
      return masker.mask(value);
    } catch (RuntimeException ex) {
      log.warn("Mask strategy {} failed — degrading to FULL_REDACT: {}", strategy, ex.toString());
      return maskerRegistry.get(MaskStrategy.FULL_REDACT).mask(value);
    }
  }
}
