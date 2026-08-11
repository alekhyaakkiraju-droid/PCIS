package com.pcis.golden;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Describes a mutated table to capture after a batch run.
 */
public record TableDefinition(
    String tableName, List<String> businessKeys, Map<String, String> columnTypes) {

  public TableDefinition {
    tableName = tableName.toUpperCase(Locale.ROOT);
    businessKeys = List.copyOf(businessKeys);
    if (columnTypes == null || columnTypes.isEmpty()) {
      columnTypes = Map.of();
    } else {
      java.util.LinkedHashMap<String, String> normalized = new java.util.LinkedHashMap<>();
      for (Map.Entry<String, String> e : columnTypes.entrySet()) {
        normalized.put(e.getKey().toUpperCase(Locale.ROOT), e.getValue());
      }
      columnTypes = Map.copyOf(normalized);
    }
  }

  public String captureSql() {
    return OrderByEnforcer.selectOrdered(tableName, businessKeys);
  }
}
