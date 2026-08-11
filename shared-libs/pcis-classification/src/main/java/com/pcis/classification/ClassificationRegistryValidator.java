package com.pcis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Validates registry YAML structure, enums, and key uniqueness. */
public final class ClassificationRegistryValidator {

  private ClassificationRegistryValidator() {}

  public static List<ClassificationEntry> validateAndFlatten(ClassificationRegistryDocument document) {
    if (document == null) {
      throw new ClassificationRegistryException("Classification registry document is null");
    }
    if (document.registryVersion() == null || document.registryVersion().isBlank()) {
      throw new ClassificationRegistryException("registry_version is required");
    }
    if (document.entities() == null || document.entities().isEmpty()) {
      throw new ClassificationRegistryException("entities section must not be empty");
    }

    Map<DataTier, Integer> retentionByTier = document.retentionDaysByTier();
    List<ClassificationEntry> entries = new ArrayList<>();
    var seenKeys = new java.util.HashSet<String>();

    for (ClassificationRegistryDocument.EntityDefinition entity : document.entities()) {
      if (entity.entity() == null || entity.entity().isBlank()) {
        throw new ClassificationRegistryException("Entity name must not be blank");
      }
      DataTier entityTier = DataTier.fromYaml(entity.tier());
      int entityRetention =
          retentionByTier.getOrDefault(entityTier, defaultRetention(entityTier));

      if (entity.columns() == null || entity.columns().isEmpty()) {
        throw new ClassificationRegistryException(
            "Entity " + entity.entity() + " has no classified columns");
      }

      for (ClassificationRegistryDocument.ColumnDefinition column : entity.columns()) {
        if (column.column() == null || column.column().isBlank()) {
          throw new ClassificationRegistryException(
              "Blank column name on entity " + entity.entity());
        }

        String key = entity.entity().toUpperCase() + ":" + column.column().toUpperCase();
        if (!seenKeys.add(key)) {
          throw new ClassificationRegistryException("Duplicate classification key: " + key);
        }

        DataTier columnTier =
            column.tier() != null && !column.tier().isBlank()
                ? DataTier.fromYaml(column.tier())
                : entityTier;
        MaskStrategy mask = MaskStrategy.fromYaml(column.maskStrategy());

        if (columnTier == DataTier.RESTRICTED && column.pii() && mask == MaskStrategy.NONE) {
          throw new ClassificationRegistryException(
              "Restricted PII column "
                  + entity.entity()
                  + "."
                  + column.column()
                  + " must declare a non-NONE mask_strategy");
        }

        int retention = retentionByTier.getOrDefault(columnTier, entityRetention);
        entries.add(
            new ClassificationEntry(
                entity.entity().toUpperCase(),
                column.column().toUpperCase(),
                columnTier,
                mask,
                retention,
                column.pii(),
                column.discriminatorColumn(),
                column.rationale()));
      }
    }

    return List.copyOf(entries);
  }

  private static int defaultRetention(DataTier tier) {
    return switch (tier) {
      case PUBLIC -> 365;
      case INTERNAL, RESTRICTED -> 2555;
      case CONFIDENTIAL -> 1825;
    };
  }
}
