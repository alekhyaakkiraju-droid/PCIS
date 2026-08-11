package com.pcis.schema.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses PostgreSQL CREATE TABLE blocks from Flyway V1 baseline SQL.
 */
public final class FlywaySchemaParser {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([A-Za-z][A-Za-z0-9_]*)\\s*\\((.*?)\\)\\s*;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern NUMERIC_TYPE = Pattern.compile("NUMERIC\\((\\d+),(\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private FlywaySchemaParser() {}

    public record FlywayColumn(String name, String dataType, int precision, int scale) {}

    public static Map<String, Map<String, FlywayColumn>> parse(Path flywaySql) throws IOException {
        String text = Files.readString(flywaySql);
        Map<String, Map<String, FlywayColumn>> tables = new LinkedHashMap<>();

        Matcher tableMatcher = CREATE_TABLE.matcher(text);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1).toUpperCase(Locale.ROOT);
            String body = tableMatcher.group(2);
            Map<String, FlywayColumn> columns = new HashMap<>();

            for (String rawLine : body.split("\\R")) {
                String line = rawLine.split("--", 2)[0].trim();
                if (line.isEmpty() || line.startsWith("(") || line.startsWith(")")) {
                    continue;
                }
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1).trim();
                }
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("PRIMARY KEY")
                        || upper.startsWith("CONSTRAINT")
                        || upper.startsWith("UNIQUE")
                        || upper.startsWith("CHECK")
                        || upper.startsWith("FOREIGN KEY")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                String columnName = parts[0].toUpperCase(Locale.ROOT);
                StringBuilder typeBuilder = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    String token = parts[i];
                    if (isTypeTerminator(token)) {
                        break;
                    }
                    if (!typeBuilder.isEmpty()) {
                        typeBuilder.append(' ');
                    }
                    typeBuilder.append(token);
                }
                String normalizedType = normalizeType(typeBuilder.toString());
                Matcher numeric = NUMERIC_TYPE.matcher(normalizedType);
                if (numeric.matches()) {
                    columns.put(
                            columnName.toLowerCase(Locale.ROOT),
                            new FlywayColumn(
                                    columnName,
                                    normalizedType,
                                    Integer.parseInt(numeric.group(1)),
                                    Integer.parseInt(numeric.group(2))));
                }
            }
            tables.put(tableName.toLowerCase(Locale.ROOT), columns);
        }
        return Map.copyOf(tables);
    }

    private static boolean isTypeTerminator(String token) {
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "NOT", "NULL", "DEFAULT", "GENERATED", "PRIMARY", "REFERENCES", "CONSTRAINT", "CHECK", "UNIQUE" ->
                    true;
            default -> false;
        };
    }

    private static String normalizeType(String raw) {
        return raw.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
