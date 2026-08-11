package com.pcis.schema.classification;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EntityClassificationManifest {

    private final int manifestVersion;
    private final List<EntityClassificationEntry> entries;

    public EntityClassificationManifest(int manifestVersion, List<EntityClassificationEntry> entries) {
        this.manifestVersion = manifestVersion;
        this.entries = List.copyOf(entries);
    }

    public int manifestVersion() {
        return manifestVersion;
    }

    public List<EntityClassificationEntry> entries() {
        return entries;
    }

    public Map<String, EntityClassificationEntry> entriesByTableName() {
        return entries.stream()
                .collect(Collectors.toMap(
                        EntityClassificationEntry::tableName,
                        entry -> entry,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public static EntityClassificationManifest load(Path manifestPath) throws IOException {
        try (InputStream in = Files.newInputStream(manifestPath)) {
            return parse(in);
        }
    }

    public static EntityClassificationManifest parse(InputStream inputStream) {
        Object root = new Yaml().load(inputStream);
        if (!(root instanceof Map<?, ?> document)) {
            throw new ManifestParseException("Manifest root must be a YAML mapping");
        }

        Object versionValue = document.get("manifest_version");
        if (!(versionValue instanceof Number versionNumber)) {
            throw new ManifestParseException("Missing or invalid manifest_version");
        }

        Object entriesValue = document.get("entries");
        if (!(entriesValue instanceof List<?> rawEntries)) {
            throw new ManifestParseException("Missing or invalid entries list");
        }

        List<EntityClassificationEntry> entries = new ArrayList<>();
        Set<String> seenTables = new LinkedHashSet<>();
        for (int index = 0; index < rawEntries.size(); index++) {
            Object item = rawEntries.get(index);
            if (!(item instanceof Map<?, ?> entryMap)) {
                throw new ManifestParseException("Entry at index " + index + " must be a mapping");
            }
            EntityClassificationEntry entry = parseEntry(entryMap, index);
            if (!seenTables.add(entry.tableName())) {
                throw new ManifestParseException("Duplicate table_name: " + entry.tableName());
            }
            entries.add(entry);
        }

        return new EntityClassificationManifest(versionNumber.intValue(), entries);
    }

    private static EntityClassificationEntry parseEntry(Map<?, ?> entryMap, int index) {
        String tableName = requiredString(entryMap, "table_name", index);
        String tierValue = requiredString(entryMap, "classification_tier", index);
        ClassificationTier tier = ClassificationTier.parse(tierValue)
                .orElseThrow(() -> new ManifestParseException(
                        "Invalid classification_tier at index " + index + ": " + tierValue));

        Object retentionValue = entryMap.get("retention_days");
        if (!(retentionValue instanceof Number retentionNumber)) {
            throw new ManifestParseException("Missing or invalid retention_days at index " + index);
        }
        int retentionDays = retentionNumber.intValue();
        if (retentionDays <= 0) {
            throw new ManifestParseException("retention_days must be positive at index " + index);
        }
        if (!tier.requiresMinimumRetention(retentionDays)) {
            throw new ManifestParseException(
                    tier + " entry " + tableName + " requires retention_days >= 365, got " + retentionDays);
        }

        List<String> piiColumns = parsePiiColumns(entryMap.get("pii_columns"), index);
        String description = optionalString(entryMap.get("description"));

        return new EntityClassificationEntry(
                tableName,
                tier,
                retentionDays,
                piiColumns,
                description);
    }

    private static List<String> parsePiiColumns(Object value, int index) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new ManifestParseException("pii_columns at index " + index + " must be a list");
        }
        List<String> columns = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof String column)) {
                throw new ManifestParseException("pii_columns at index " + index + " must contain strings");
            }
            columns.add(column);
        }
        return columns;
    }

    private static String requiredString(Map<?, ?> entryMap, String field, int index) {
        Object value = entryMap.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ManifestParseException("Missing or invalid " + field + " at index " + index);
        }
        return text.trim().toUpperCase(Locale.ROOT);
    }

    private static String optionalString(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    public List<String> validateCompleteness(Set<String> schemaTables) {
        Set<String> normalizedSchema = schemaTables.stream()
                .map(EntityClassificationEntry::normalizeTableName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> manifestTables = entries.stream()
                .map(EntityClassificationEntry::tableName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> errors = new ArrayList<>();
        for (String table : normalizedSchema) {
            if (!manifestTables.contains(table)) {
                errors.add("Unclassified schema table: " + table);
            }
        }
        for (String table : manifestTables) {
            if (!normalizedSchema.contains(table)) {
                errors.add("Manifest entry not present in schema: " + table);
            }
        }
        return errors;
    }

    public static Path resolveManifestPath() {
        List<Path> candidates = List.of(
                Path.of("docs/entity-classification.yaml"),
                Path.of("..", "..", "docs", "entity-classification.yaml"),
                Path.of("..", "..", "..", "docs", "entity-classification.yaml"));
        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(absolute)) {
                return absolute;
            }
        }
        throw new IllegalStateException(
                "Unable to locate docs/entity-classification.yaml from working directory "
                        + Path.of("").toAbsolutePath());
    }

    public static class ManifestParseException extends RuntimeException {
        public ManifestParseException(String message) {
            super(message);
        }
    }
}
