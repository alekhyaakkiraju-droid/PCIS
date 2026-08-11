package com.pcis.sync.load;

import com.pcis.sync.config.SyncAgentProperties;
import com.pcis.sync.extract.SourceExtractService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TargetUpsertService {

  private final JdbcTemplate targetJdbcTemplate;
  private final SourceExtractService extractService;

  public TargetUpsertService(
      @Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate,
      SourceExtractService extractService) {
    this.targetJdbcTemplate = targetJdbcTemplate;
    this.extractService = extractService;
  }

  public int upsertBatch(SyncAgentProperties.DomainProperties domain, List<Map<String, Object>> rows) {
    if (rows.isEmpty()) {
      return 0;
    }

    List<String> sourceColumns = domain.getColumns();
    List<String> targetColumns = toTargetColumns(sourceColumns);
    String primaryKey = toTargetColumn(domain.getPrimaryKey());

    String columnList = String.join(", ", targetColumns);
    String placeholders = targetColumns.stream().map(c -> "?").collect(Collectors.joining(", "));
    String updateSet =
        targetColumns.stream()
            .filter(c -> !c.equals(primaryKey))
            .map(c -> c + " = EXCLUDED." + c)
            .collect(Collectors.joining(", "));

    String sql =
        """
        INSERT INTO %s (%s)
        VALUES (%s)
        ON CONFLICT (%s) DO UPDATE SET %s, synced_at = NOW()
        """
            .formatted(domain.getTargetTable(), columnList, placeholders, primaryKey, updateSet);

    int upserted = 0;
    for (Map<String, Object> row : rows) {
      List<Object> values = extractService.rowValues(sourceColumns, row);
      upserted += targetJdbcTemplate.update(sql, values.toArray());
    }
    return upserted;
  }

  private List<String> toTargetColumns(List<String> sourceColumns) {
    List<String> targetColumns = new ArrayList<>(sourceColumns.size());
    for (String column : sourceColumns) {
      targetColumns.add(toTargetColumn(column));
    }
    return targetColumns;
  }

  private String toTargetColumn(String sourceColumn) {
    return sourceColumn.toLowerCase();
  }
}
