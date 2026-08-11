package com.pcis.schema.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds monetary column expectations from the data dictionary, resolved against Flyway V1 SQL.
 */
public final class MonetaryColumnSpecResolver {

    private static final Pattern NUMERIC_TYPE = Pattern.compile("NUMERIC\\((\\d+),(\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private MonetaryColumnSpecResolver() {}

    public record DictionaryEntry(String tableName, String ddlColumnName, String resolution, String targetPgType) {}

    public static List<MonetaryColumnSpec> resolve(Path dictionaryPath, Path flywaySql) throws IOException {
        List<DictionaryEntry> dictionaryEntries = loadDictionaryMonetaryEntries(dictionaryPath);
        Map<String, Map<String, FlywaySchemaParser.FlywayColumn>> flyway =
                FlywaySchemaParser.parse(flywaySql);

        List<MonetaryColumnSpec> specs = new ArrayList<>();
        for (DictionaryEntry entry : dictionaryEntries) {
            String tableKey = entry.tableName().toLowerCase(Locale.ROOT);
            Map<String, FlywaySchemaParser.FlywayColumn> tableColumns = flyway.get(tableKey);
            if (tableColumns == null) {
                continue;
            }

            String flywayColumnName = flywayColumnName(entry);
            FlywaySchemaParser.FlywayColumn flywayColumn = tableColumns.get(flywayColumnName.toLowerCase(Locale.ROOT));
            if (flywayColumn == null) {
                continue;
            }

            MonetaryKind kind = flywayColumn.scale() == 4 ? MonetaryKind.RATE_FACTOR : MonetaryKind.AMOUNT;
            specs.add(new MonetaryColumnSpec(
                    entry.tableName(),
                    flywayColumnName,
                    flywayColumn.precision(),
                    flywayColumn.scale(),
                    kind));
        }

        specs.sort(Comparator
                .comparing(MonetaryColumnSpec::tableName)
                .thenComparing(MonetaryColumnSpec::columnName));
        return List.copyOf(specs);
    }

    static List<DictionaryEntry> loadDictionaryMonetaryEntries(Path dictionaryPath) throws IOException {
        String currentTable = null;
        String ddlColumnName = null;
        String resolution = null;
        List<DictionaryEntry> entries = new ArrayList<>();

        for (String line : Files.readAllLines(dictionaryPath)) {
            if (line.startsWith("  - table_name: ")) {
                currentTable = line.substring("  - table_name: ".length()).trim();
                ddlColumnName = null;
                resolution = null;
                continue;
            }
            if (line.startsWith("      - ddl_column_name: ")) {
                ddlColumnName = line.substring("      - ddl_column_name: ".length()).trim();
                continue;
            }
            if (line.startsWith("        resolution: ")) {
                resolution = line.substring("        resolution: ".length()).trim();
                continue;
            }
            if (line.startsWith("        target_pg_type: ") && currentTable != null) {
                String rawType = extractValue(line.substring("        target_pg_type: ".length()));
                Matcher matcher = NUMERIC_TYPE.matcher(rawType);
                if (matcher.matches()) {
                    int scale = Integer.parseInt(matcher.group(2));
                    if (scale == 2 || scale == 4) {
                        entries.add(new DictionaryEntry(
                                currentTable,
                                ddlColumnName,
                                resolution,
                                rawType));
                    }
                }
                ddlColumnName = null;
                resolution = null;
            }
        }
        return List.copyOf(entries);
    }

    private static String flywayColumnName(DictionaryEntry entry) {
        if (entry.ddlColumnName() != null && !entry.ddlColumnName().isBlank()) {
            return entry.ddlColumnName().toLowerCase(Locale.ROOT);
        }
        return entry.resolution().toLowerCase(Locale.ROOT);
    }

    private static String extractValue(String remainder) {
        String trimmed = remainder.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
