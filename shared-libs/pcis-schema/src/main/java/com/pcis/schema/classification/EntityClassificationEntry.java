package com.pcis.schema.classification;

import java.util.List;
import java.util.Locale;

public record EntityClassificationEntry(
        String tableName,
        ClassificationTier classificationTier,
        int retentionDays,
        List<String> piiColumns,
        String description) {

    public EntityClassificationEntry {
        tableName = normalizeTableName(tableName);
        piiColumns = piiColumns == null
                ? List.of()
                : List.copyOf(piiColumns.stream()
                        .map(col -> col == null ? "" : col.trim().toUpperCase(Locale.ROOT))
                        .filter(col -> !col.isEmpty())
                        .toList());
        description = description == null ? "" : description.trim();
    }

    public static String normalizeTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String normalized = tableName.trim().toUpperCase(Locale.ROOT);
        if ("OUTBOX_EVENTS".equals(normalized)) {
            return normalized;
        }
        return normalized;
    }
}
