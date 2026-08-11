package com.pcis.schema.migration;

/**
 * Expected PostgreSQL monetary column from {@code docs/data-dictionary.yaml}.
 */
public record MonetaryColumnSpec(
        String tableName,
        String columnName,
        int precision,
        int scale,
        MonetaryKind kind) {

    public String qualifiedName() {
        return tableName.toLowerCase() + "." + columnName.toLowerCase();
    }

    public String expectedPgType() {
        return "NUMERIC(" + precision + "," + scale + ")";
    }
}
