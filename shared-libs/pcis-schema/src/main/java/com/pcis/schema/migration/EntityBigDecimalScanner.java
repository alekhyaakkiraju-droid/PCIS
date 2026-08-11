package com.pcis.schema.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans {@code @Entity} source files and verifies monetary columns use {@code BigDecimal}.
 */
public final class EntityBigDecimalScanner {

    private static final Pattern TABLE_NAME = Pattern.compile("@Table\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern CLASS_NAME = Pattern.compile("class\\s+(\\w+)");
    private static final Pattern COLUMN_FIELD = Pattern.compile(
            "@Column\\([^)]*name\\s*=\\s*\"([^\"]+)\"[^)]*\\)"
                    + "[\\s\\S]*?"
                    + "private\\s+([\\w.<>, ?]+)\\s+\\w+\\s*;",
            Pattern.MULTILINE);

    private static final Set<String> FORBIDDEN_JAVA_TYPES = Set.of(
            "float", "double", "Float", "Double", "BigInteger");

    public record EntityFieldMapping(String sourceFile, String tableName, String columnName, String javaType) {}

    public record EntityCheckResult(EntityFieldMapping mapping, boolean passed, String message) {
        public String formatLine() {
            return mapping.tableName().toLowerCase(Locale.ROOT)
                    + '.'
                    + mapping.columnName().toLowerCase(Locale.ROOT)
                    + " ("
                    + mapping.sourceFile()
                    + "): "
                    + (passed ? "PASS" : "FAIL")
                    + " — "
                    + message;
        }
    }

    public List<EntityFieldMapping> scanRepo(Path repoRoot) throws IOException {
        List<Path> roots = List.of(
                repoRoot.resolve("services"),
                repoRoot.resolve("shared-libs"));
        List<EntityFieldMapping> mappings = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.toString().contains("/target/"))
                        .filter(p -> !p.toString().contains("/test/"))
                        .forEach(p -> {
                            try {
                                mappings.addAll(scanFile(p, repoRoot));
                            } catch (IOException e) {
                                throw new IllegalStateException("Failed to scan " + p, e);
                            }
                        });
            }
        }
        return List.copyOf(mappings);
    }

    public List<EntityFieldMapping> scanSource(String source, String sourceLabel) {
        if (!source.contains("@Entity")) {
            return List.of();
        }
        String tableName = extractTableName(source);
        List<EntityFieldMapping> mappings = new ArrayList<>();
        Matcher matcher = COLUMN_FIELD.matcher(source);
        while (matcher.find()) {
            mappings.add(new EntityFieldMapping(
                    sourceLabel,
                    tableName,
                    matcher.group(1),
                    normalizeType(matcher.group(2))));
        }
        return List.copyOf(mappings);
    }

    List<EntityFieldMapping> scanFile(Path file, Path repoRoot) throws IOException {
        String source = Files.readString(file);
        if (!source.contains("@Entity")) {
            return List.of();
        }
        String relative = repoRoot.relativize(file).toString();
        return scanSource(source, relative).stream()
                .map(m -> new EntityFieldMapping(relative, m.tableName(), m.columnName(), m.javaType()))
                .toList();
    }

    public List<EntityCheckResult> validateMonetaryMappings(
            List<EntityFieldMapping> mappings, List<MonetaryColumnSpec> monetaryColumns) {
        Map<String, MonetaryColumnSpec> monetaryByColumn = new HashMap<>();
        for (MonetaryColumnSpec spec : monetaryColumns) {
            monetaryByColumn.put(
                    spec.tableName().toLowerCase(Locale.ROOT) + "." + spec.columnName().toLowerCase(Locale.ROOT),
                    spec);
        }

        Set<String> seen = new HashSet<>();
        List<EntityCheckResult> results = new ArrayList<>();
        for (EntityFieldMapping mapping : mappings) {
            String key = mapping.tableName().toLowerCase(Locale.ROOT)
                    + "."
                    + mapping.columnName().toLowerCase(Locale.ROOT);
            MonetaryColumnSpec spec = monetaryByColumn.get(key);
            if (spec == null) {
                continue;
            }
            seen.add(key);
            results.add(validateMapping(mapping, spec));
        }
        return List.copyOf(results);
    }

    public EntityCheckResult validateMapping(EntityFieldMapping mapping, MonetaryColumnSpec spec) {
        String javaType = mapping.javaType();
        if (FORBIDDEN_JAVA_TYPES.contains(javaType)) {
            return new EntityCheckResult(
                    mapping,
                    false,
                    "monetary column must use BigDecimal, found " + javaType);
        }
        if (!"BigDecimal".equals(javaType)) {
            return new EntityCheckResult(
                    mapping,
                    false,
                    "monetary column must use BigDecimal, found " + javaType);
        }
        return new EntityCheckResult(
                mapping,
                true,
                "BigDecimal mapped to " + spec.expectedPgType() + " (" + spec.kind() + ")");
    }

    private static String extractTableName(String source) {
        Matcher tableMatcher = TABLE_NAME.matcher(source);
        if (tableMatcher.find()) {
            return tableMatcher.group(1);
        }
        Matcher classMatcher = CLASS_NAME.matcher(source);
        if (classMatcher.find()) {
            return camelToSnake(classMatcher.group(1)).toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    private static String normalizeType(String raw) {
        return raw.trim().replaceAll("\\s+", "");
    }

    private static String camelToSnake(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
