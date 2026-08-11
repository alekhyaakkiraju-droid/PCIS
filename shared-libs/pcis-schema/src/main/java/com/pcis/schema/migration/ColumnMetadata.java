package com.pcis.schema.migration;

import java.util.Optional;

/**
 * Column type information from {@code information_schema.columns} or a test stub.
 */
public record ColumnMetadata(String dataType, Integer numericPrecision, Integer numericScale) {

    public Optional<Integer> precision() {
        return Optional.ofNullable(numericPrecision);
    }

    public Optional<Integer> scale() {
        return Optional.ofNullable(numericScale);
    }
}
