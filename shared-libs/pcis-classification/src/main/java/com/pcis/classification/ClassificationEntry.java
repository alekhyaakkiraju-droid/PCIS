package com.pcis.classification;

/** Immutable column-level classification entry. */
public record ClassificationEntry(
    String entityName,
    String columnName,
    DataTier tier,
    MaskStrategy maskStrategy,
    int retentionDays,
    boolean pii,
    String discriminatorColumn,
    String rationale) {

  public String cacheKey() {
    return entityName.toUpperCase() + ":" + columnName.toUpperCase();
  }
}
