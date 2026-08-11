package com.pcis.golden;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects attempts to add monetary NUMERIC(9,2)/(11,2) or status columns to the
 * normalization allow-list. Monetary and status values must remain exact for
 * cent-level parity.
 */
public final class NormalizationConfigValidator {

  private final NormalizationRules rules;

  public NormalizationConfigValidator(NormalizationRules rules) {
    this.rules = rules;
  }

  /**
   * Validates that every proposed allow-list column is permitted.
   *
   * @throws ConfigurationException if any column is on the deny list
   */
  public void validateAllowList(Collection<String> proposedAllowColumns) {
    Set<String> rejected = new LinkedHashSet<>();
    for (String column : proposedAllowColumns) {
      if (column == null || column.isBlank()) {
        continue;
      }
      String col = column.trim().toUpperCase(Locale.ROOT);
      if (rules.isDenied(col)) {
        rejected.add(col);
      }
    }
    if (!rejected.isEmpty()) {
      throw new ConfigurationException(
          "Normalization allow-list rejects monetary/status columns: "
              + rejected
              + ". Deny-list monetary types="
              + rules.denyMonetaryTypes()
              + ", monetary columns="
              + rules.denyMonetaryColumns()
              + ", status columns="
              + rules.denyStatusColumns());
    }
  }

  /**
   * Validates the rules file itself: allow-list entries must not intersect deny-list.
   */
  public void validateRulesConsistency() {
    Set<String> proposed = new LinkedHashSet<>();
    proposed.addAll(rules.allowTimestamps());
    proposed.addAll(rules.allowSurrogates());
    validateAllowList(proposed);
  }
}
