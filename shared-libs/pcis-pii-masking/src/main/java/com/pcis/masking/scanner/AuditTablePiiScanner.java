package com.pcis.masking.scanner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** JDBC scanner for audit table text columns using streaming ResultSets. */
public final class AuditTablePiiScanner {

  private static final int DEFAULT_FETCH_SIZE = 1000;
  private static final List<String> DEFAULT_AUDIT_COLUMNS =
      List.of("OLD_VALUE", "NEW_VALUE", "RECORD_KEY");

  private final PiiScanner textScanner = new PiiScanner();
  private final int fetchSize;

  public AuditTablePiiScanner() {
    this(DEFAULT_FETCH_SIZE);
  }

  public AuditTablePiiScanner(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  public List<PiiDetection> scanTable(DataSource dataSource, String tableName) {
    return scanTable(dataSource, tableName, DEFAULT_AUDIT_COLUMNS);
  }

  public List<PiiDetection> scanTable(
      DataSource dataSource, String tableName, List<String> textColumns) {
    if (textColumns == null || textColumns.isEmpty()) {
      return List.of();
    }
    if (!tableExists(dataSource, tableName)) {
      return List.of();
    }

    String sql = buildSelectSql(tableName, textColumns);
    List<PiiDetection> detections = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setFetchSize(fetchSize);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          String rowId = String.valueOf(resultSet.getLong("LOG_ID"));
          for (String column : textColumns) {
            String value = resultSet.getString(column);
            detections.addAll(textScanner.scanText(tableName, column, rowId, value));
          }
        }
      }
    } catch (SQLException ex) {
      throw new PiiScanException("Failed scanning table " + tableName, ex);
    }
    return detections;
  }

  public PiiScanReport scanAuditTables(DataSource dataSource, List<String> tableNames) {
    List<PiiDetection> detections = new ArrayList<>();
    long rowsScanned = 0;
    int tablesScanned = 0;
    for (String tableName : tableNames) {
      if (!tableExists(dataSource, tableName)) {
        continue;
      }
      tablesScanned++;
      rowsScanned += countRows(dataSource, tableName);
      detections.addAll(scanTable(dataSource, tableName));
    }
    return PiiScanReport.of(tablesScanned, rowsScanned, 0, detections);
  }

  private static boolean tableExists(DataSource dataSource, String tableName) {
    String sql =
        "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND lower(table_name) = lower(?)";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, tableName);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    } catch (SQLException ex) {
      throw new PiiScanException("Unable to check table existence for " + tableName, ex);
    }
  }

  private static long countRows(DataSource dataSource, String tableName) {
    String sql = "SELECT COUNT(*) FROM " + tableName;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      resultSet.next();
      return resultSet.getLong(1);
    } catch (SQLException ex) {
      throw new PiiScanException("Unable to count rows for " + tableName, ex);
    }
  }

  private static String buildSelectSql(String tableName, List<String> textColumns) {
    StringBuilder columns = new StringBuilder("LOG_ID");
    for (String column : textColumns) {
      columns.append(", ").append(column);
    }
    return "SELECT " + columns + " FROM " + tableName;
  }
}
