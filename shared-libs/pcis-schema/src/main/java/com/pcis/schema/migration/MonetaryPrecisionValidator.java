package com.pcis.schema.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates monetary columns against schema metadata (WO-152 CI gate).
 */
public final class MonetaryPrecisionValidator {

    public static final Set<String> FORBIDDEN_TYPES = Set.of(
            "float4",
            "float8",
            "money",
            "real",
            "double precision");

    public List<ColumnCheckResult> validateAll(
            List<MonetaryColumnSpec> specs, ColumnMetadataProvider metadata) {
        List<ColumnCheckResult> results = new ArrayList<>(specs.size());
        for (MonetaryColumnSpec spec : specs) {
            results.add(validateOne(spec, metadata));
        }
        return List.copyOf(results);
    }

    public ColumnCheckResult validateOne(MonetaryColumnSpec spec, ColumnMetadataProvider metadata) {
        Optional<ColumnMetadata> actualOpt =
                metadata.getColumn(spec.tableName(), spec.columnName());
        if (actualOpt.isEmpty()) {
            return new ColumnCheckResult(
                    spec.tableName(),
                    spec.columnName(),
                    false,
                    "column missing from schema",
                    spec,
                    null,
                    null,
                    null);
        }

        ColumnMetadata actual = actualOpt.get();
        String dataType = actual.dataType().toLowerCase(Locale.ROOT);

        if (FORBIDDEN_TYPES.contains(dataType)) {
            return new ColumnCheckResult(
                    spec.tableName(),
                    spec.columnName(),
                    false,
                    "forbidden floating-point or money type",
                    spec,
                    actual.dataType(),
                    actual.numericPrecision(),
                    actual.numericScale());
        }

        if (!"numeric".equals(dataType)) {
            return new ColumnCheckResult(
                    spec.tableName(),
                    spec.columnName(),
                    false,
                    "expected NUMERIC, got " + actual.dataType(),
                    spec,
                    actual.dataType(),
                    actual.numericPrecision(),
                    actual.numericScale());
        }

        Integer precision = actual.numericPrecision();
        Integer scale = actual.numericScale();
        if (precision == null || scale == null) {
            return new ColumnCheckResult(
                    spec.tableName(),
                    spec.columnName(),
                    false,
                    "numeric column missing precision/scale metadata",
                    spec,
                    actual.dataType(),
                    precision,
                    scale);
        }

        if (precision != spec.precision() || scale != spec.scale()) {
            return new ColumnCheckResult(
                    spec.tableName(),
                    spec.columnName(),
                    false,
                    "precision/scale mismatch",
                    spec,
                    actual.dataType(),
                    precision,
                    scale);
        }

        return new ColumnCheckResult(
                spec.tableName(),
                spec.columnName(),
                true,
                "matches data dictionary",
                spec,
                actual.dataType(),
                precision,
                scale);
    }

    public MonetaryPrecisionReport buildReport(List<ColumnCheckResult> results) {
        return new MonetaryPrecisionReport(results);
    }

    public static final class MonetaryPrecisionReport {
        private final List<ColumnCheckResult> results;

        MonetaryPrecisionReport(List<ColumnCheckResult> results) {
            this.results = List.copyOf(results);
        }

        public List<ColumnCheckResult> results() {
            return results;
        }

        public long passCount() {
            return results.stream().filter(ColumnCheckResult::passed).count();
        }

        public long failCount() {
            return results.stream().filter(r -> !r.passed()).count();
        }

        public boolean allPassed() {
            return failCount() == 0;
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("MONETARY PRECISION GATE REPORT").append(System.lineSeparator());
            sb.append("==============================").append(System.lineSeparator());
            for (ColumnCheckResult result : results) {
                sb.append(result.formatLine()).append(System.lineSeparator());
            }
            sb.append("------------------------------").append(System.lineSeparator());
            sb.append("Summary: ")
                    .append(passCount())
                    .append(" PASS, ")
                    .append(failCount())
                    .append(" FAIL")
                    .append(System.lineSeparator());
            return sb.toString();
        }
    }

    public static ColumnMetadataProvider fromMap(Map<String, ColumnMetadata> byQualifiedName) {
        return (table, column) -> Optional.ofNullable(
                byQualifiedName.get(table.toLowerCase(Locale.ROOT) + "." + column.toLowerCase(Locale.ROOT)));
    }
}
