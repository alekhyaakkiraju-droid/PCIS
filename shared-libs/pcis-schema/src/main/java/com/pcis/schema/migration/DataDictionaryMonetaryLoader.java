package com.pcis.schema.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads monetary column expectations from {@code docs/data-dictionary.yaml}.
 * Includes NUMERIC columns with scale 2 (amounts) and scale 4 (rate factors).
 */
public final class DataDictionaryMonetaryLoader {

    private static final Pattern NUMERIC_TYPE = Pattern.compile("NUMERIC\\((\\d+),(\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private DataDictionaryMonetaryLoader() {}

    public static List<MonetaryColumnSpec> load(Path dictionaryPath) throws IOException {
        String currentTable = null;
        String resolution = null;
        List<MonetaryColumnSpec> specs = new ArrayList<>();

        for (String line : Files.readAllLines(dictionaryPath)) {
            if (line.startsWith("  - table_name: ")) {
                currentTable = line.substring("  - table_name: ".length()).trim();
                resolution = null;
                continue;
            }
            if (line.startsWith("        resolution: ")) {
                resolution = line.substring("        resolution: ".length()).trim();
                continue;
            }
            if (line.startsWith("        target_pg_type: ") && currentTable != null && resolution != null) {
                String rawType = extractValue(line.substring("        target_pg_type: ".length()));
                Matcher matcher = NUMERIC_TYPE.matcher(rawType);
                if (matcher.matches()) {
                    int precision = Integer.parseInt(matcher.group(1));
                    int scale = Integer.parseInt(matcher.group(2));
                    MonetaryKind kind = scale == 4 ? MonetaryKind.RATE_FACTOR : MonetaryKind.AMOUNT;
                    if (scale == 2 || scale == 4) {
                        specs.add(new MonetaryColumnSpec(
                                currentTable,
                                resolution,
                                precision,
                                scale,
                                kind));
                    }
                }
                resolution = null;
            }
        }

        specs.sort(Comparator
                .comparing(MonetaryColumnSpec::tableName)
                .thenComparing(MonetaryColumnSpec::columnName));
        return List.copyOf(specs);
    }

    private static String extractValue(String remainder) {
        String trimmed = remainder.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
