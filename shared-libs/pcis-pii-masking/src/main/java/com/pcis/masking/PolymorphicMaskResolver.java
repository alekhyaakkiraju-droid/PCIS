package com.pcis.masking;

import com.pcis.classification.ClassificationEntry;
import com.pcis.classification.MaskStrategy;
import java.util.Locale;
import java.util.Optional;

/** Resolves polymorphic mask strategies from discriminator column values. */
public final class PolymorphicMaskResolver {

  public Optional<MaskStrategy> resolve(ClassificationEntry entry, String discriminatorValue) {
    if (entry.discriminatorColumn() == null || entry.discriminatorColumn().isBlank()) {
      return Optional.empty();
    }
    if (discriminatorValue == null || discriminatorValue.isBlank()) {
      return Optional.of(MaskStrategy.FULL_REDACT);
    }
    if (!isContactValue(entry)) {
      return Optional.empty();
    }
    return Optional.of(resolveContactValueStrategy(discriminatorValue));
  }

  private static boolean isContactValue(ClassificationEntry entry) {
    return "CUSTOMER_CONTACT_T".equalsIgnoreCase(entry.entityName())
        && "CONTACT_VALUE".equalsIgnoreCase(entry.columnName());
  }

  private static MaskStrategy resolveContactValueStrategy(String discriminatorValue) {
    return switch (discriminatorValue.trim().toUpperCase(Locale.ROOT)) {
      case "EM" -> MaskStrategy.EMAIL_DOMAIN_ONLY;
      case "PH", "MB" -> MaskStrategy.PHONE_LAST_FOUR;
      default -> MaskStrategy.FULL_REDACT;
    };
  }
}
