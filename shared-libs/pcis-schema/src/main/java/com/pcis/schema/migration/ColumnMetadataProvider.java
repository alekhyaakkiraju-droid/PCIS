package com.pcis.schema.migration;

import java.util.Optional;

@FunctionalInterface
public interface ColumnMetadataProvider {

    Optional<ColumnMetadata> getColumn(String tableName, String columnName);
}
