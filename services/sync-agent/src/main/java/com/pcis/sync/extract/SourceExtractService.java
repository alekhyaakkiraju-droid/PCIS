package com.pcis.sync.extract;

import com.pcis.sync.config.SyncAgentProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SourceExtractService {

  private final JdbcTemplate sourceJdbcTemplate;

  public SourceExtractService(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate) {
    this.sourceJdbcTemplate = sourceJdbcTemplate;
  }

  public List<Map<String, Object>> extractSinceWatermark(
      SyncAgentProperties.DomainProperties domain,
      String watermarkValue,
      int chunkSize) {
    List<String> columns = domain.getColumns();
    String columnList = String.join(", ", columns);
    String sql =
        """
        SELECT %s
        FROM %s
        WHERE %s > ?
        ORDER BY %s
        FETCH FIRST %d ROWS ONLY
        """
            .formatted(
                columnList,
                domain.getSourceTable(),
                domain.getWatermarkColumn(),
                domain.getWatermarkColumn(),
                chunkSize);

    return sourceJdbcTemplate.query(
        sql,
        (rs, rowNum) -> mapRow(rs, columns),
        watermarkValue);
  }

  private Map<String, Object> mapRow(ResultSet rs, List<String> columns) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    for (String column : columns) {
      row.put(column, rs.getObject(column));
    }
    return row;
  }

  public String maxWatermarkFromBatch(
      List<Map<String, Object>> rows, String watermarkColumn, String currentWatermark) {
    String max = currentWatermark;
    for (Map<String, Object> row : rows) {
      Object value = row.get(watermarkColumn);
      if (value != null) {
        String candidate = value.toString();
        if (candidate.compareTo(max) > 0) {
          max = candidate;
        }
      }
    }
    return max;
  }

  public List<Object> rowValues(List<String> columns, Map<String, Object> row) {
    List<Object> values = new ArrayList<>(columns.size());
    for (String column : columns) {
      values.add(row.get(column));
    }
    return values;
  }
}
