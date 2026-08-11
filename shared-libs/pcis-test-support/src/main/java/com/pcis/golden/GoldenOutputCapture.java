package com.pcis.golden;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;

/**
 * Captures post-run table images, run-log counters, and DISPLAY text into a
 * canonical golden JSON document.
 */
public final class GoldenOutputCapture {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  private final DataSource dataSource;
  private final GoldenCaptureContext context;

  public GoldenOutputCapture(DataSource dataSource, GoldenCaptureContext context) {
    this.dataSource = dataSource;
    this.context = context;
  }

  public GoldenArtifact capture(
      String program,
      String scenario,
      List<TableDefinition> tables,
      String displayOutput,
      String completionStatus)
      throws SQLException {
    context.sequenceNormalizer().reset();
    GoldenArtifact artifact = new GoldenArtifact();
    artifact.setProgram(program);
    artifact.setScenario(scenario);
    artifact.setReferenceDate(context.referenceDate().toString());
    artifact.setCompletionStatus(completionStatus);
    artifact.setDisplayOutput(displayOutput == null ? "" : displayOutput);

    List<Map<String, Object>> tableSnapshots = new ArrayList<>();
    Map<String, Object> runLog = null;

    try (Connection conn = dataSource.getConnection()) {
      for (TableDefinition table : tables) {
        Map<String, Object> snapshot = captureTable(conn, table);
        if ("RPT_RUN_LOG_T".equals(table.tableName())) {
          runLog = extractRunLog(snapshot, program);
        }
        tableSnapshots.add(snapshot);
      }
    }

    if (runLog == null) {
      runLog = new TreeMap<>();
      runLog.put("programName", program);
      runLog.put("status", completionStatus);
      runLog.put("rowsProcessed", 0);
      runLog.put("runStarted", GoldenCaptureContext.NORMALIZED_TS);
      runLog.put("runEnded", GoldenCaptureContext.NORMALIZED_TS);
    }
    artifact.setRunLog(runLog);
    artifact.setTables(tableSnapshots);
    return artifact;
  }

  public byte[] toCanonicalBytes(GoldenArtifact artifact) {
    try {
      byte[] json = MAPPER.writeValueAsBytes(artifact.toCanonicalMap());
      // Ensure trailing newline for POSIX-friendly byte identity.
      if (json.length == 0 || json[json.length - 1] != '\n') {
        byte[] withNl = new byte[json.length + 1];
        System.arraycopy(json, 0, withNl, 0, json.length);
        withNl[json.length] = '\n';
        return withNl;
      }
      return json;
    } catch (IOException e) {
      throw new ConfigurationException("Failed to serialize golden artifact", e);
    }
  }

  public void write(GoldenArtifact artifact, Path dest) throws IOException {
    Files.createDirectories(dest.getParent());
    Files.write(dest, toCanonicalBytes(artifact));
  }

  public String toCanonicalString(GoldenArtifact artifact) {
    return new String(toCanonicalBytes(artifact), StandardCharsets.UTF_8);
  }

  private Map<String, Object> captureTable(Connection conn, TableDefinition table)
      throws SQLException {
    String sql = table.captureSql();
    OrderByEnforcer.requireOrderBy(sql);

    List<Map<String, String>> columns = new ArrayList<>();
    List<Map<String, Object>> rows = new ArrayList<>();

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      ResultSetMetaData meta = rs.getMetaData();
      int colCount = meta.getColumnCount();
      List<String> names = new ArrayList<>();
      for (int i = 1; i <= colCount; i++) {
        String name = meta.getColumnLabel(i).toUpperCase(Locale.ROOT);
        names.add(name);
        String type = resolveType(table, name, meta.getColumnType(i), meta.getScale(i));
        Map<String, String> col = new TreeMap<>();
        col.put("name", name);
        col.put("type", type);
        columns.add(col);
      }

      while (rs.next()) {
        Map<String, Object> row = new TreeMap<>();
        for (int i = 1; i <= colCount; i++) {
          String name = names.get(i - 1);
          String type = columns.get(i - 1).get("type");
          Object raw = readValue(rs, i, meta.getColumnType(i));
          String normalized = context.normalizeValue(name, type, raw);
          // Keep monetary as string; integers may stay numeric-looking strings.
          row.put(name, coerceTyped(type, normalized, raw));
        }
        rows.add(row);
      }
    }

    Map<String, Object> snapshot = new TreeMap<>();
    snapshot.put("tableName", table.tableName());
    snapshot.put("businessKeys", table.businessKeys());
    snapshot.put("columns", columns);
    snapshot.put("rows", rows);
    return snapshot;
  }

  private static Object coerceTyped(String type, String normalized, Object raw) {
    if (normalized == null) {
      return "";
    }
    if ("INTEGER".equals(type) && !normalized.isEmpty() && !normalized.startsWith("SEQ_")) {
      try {
        return Integer.parseInt(normalized);
      } catch (NumberFormatException ignored) {
        return normalized;
      }
    }
    if (type != null && type.startsWith("NUMERIC") && raw instanceof BigDecimal bd) {
      // Preserve exact scale-2 money as plain string — never normalize.
      return bd.setScale(Math.max(bd.scale(), 2)).toPlainString();
    }
    if (type != null && type.startsWith("NUMERIC") && !normalized.isEmpty()) {
      return normalized;
    }
    return normalized;
  }

  private static Object readValue(ResultSet rs, int index, int jdbcType) throws SQLException {
    return switch (jdbcType) {
      case Types.NUMERIC, Types.DECIMAL -> rs.getBigDecimal(index);
      case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> {
        int v = rs.getInt(index);
        yield rs.wasNull() ? null : v;
      }
      case Types.BIGINT -> {
        long v = rs.getLong(index);
        yield rs.wasNull() ? null : v;
      }
      default -> rs.getString(index);
    };
  }

  private String resolveType(TableDefinition table, String name, int jdbcType, int scale) {
    if (table.columnTypes().containsKey(name)) {
      return table.columnTypes().get(name);
    }
    if (context.rules().isSurrogateColumn(name)) {
      return "SURROGATE";
    }
    if (context.rules().isDenied(name)
        && context.rules().denyStatusColumns().contains(name)) {
      return "STATUS";
    }
    if (jdbcType == Types.NUMERIC || jdbcType == Types.DECIMAL) {
      if (scale == 2) {
        // Prefer documented money widths when scale is 2.
        return context.rules().denyMonetaryColumns().contains(name)
            ? (name.contains("COMMISSION") ? "NUMERIC(9,2)" : "NUMERIC(11,2)")
            : "NUMERIC(11,2)";
      }
      return "NUMERIC";
    }
    if (jdbcType == Types.INTEGER
        || jdbcType == Types.SMALLINT
        || jdbcType == Types.TINYINT
        || jdbcType == Types.BIGINT) {
      return "INTEGER";
    }
    if (jdbcType == Types.DATE) {
      return "DATE";
    }
    if (jdbcType == Types.TIMESTAMP || jdbcType == Types.TIMESTAMP_WITH_TIMEZONE) {
      return "TIMESTAMP";
    }
    return "STRING";
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractRunLog(Map<String, Object> snapshot, String program) {
    List<Map<String, Object>> rows = (List<Map<String, Object>>) snapshot.get("rows");
    Map<String, Object> runLog = new TreeMap<>();
    runLog.put("programName", program);
    if (rows == null || rows.isEmpty()) {
      runLog.put("status", "UNKNOWN");
      runLog.put("rowsProcessed", 0);
      runLog.put("runStarted", GoldenCaptureContext.NORMALIZED_TS);
      runLog.put("runEnded", GoldenCaptureContext.NORMALIZED_TS);
      return runLog;
    }
    Map<String, Object> first = rows.get(0);
    runLog.put("status", String.valueOf(first.getOrDefault("STATUS", "UNKNOWN")));
    Object rowsProcessed = first.getOrDefault("ROWS_PROCESSED", 0);
    runLog.put(
        "rowsProcessed",
        rowsProcessed instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(rowsProcessed)));
    runLog.put(
        "runStarted",
        String.valueOf(first.getOrDefault("RUN_STARTED", GoldenCaptureContext.NORMALIZED_TS)));
    runLog.put(
        "runEnded",
        String.valueOf(first.getOrDefault("RUN_ENDED", GoldenCaptureContext.NORMALIZED_TS)));
    return runLog;
  }

  /** Convenience factory for common PCIS mutated tables. */
  public static List<TableDefinition> defaultTablesFor(String program) {
    return switch (program.toUpperCase(Locale.ROOT)) {
      case "BIL003B" -> List.of(
          table(
              "POLICY_T",
              List.of("POLICY_ID"),
              Map.of(
                  "ANNUAL_PREMIUM",
                  "NUMERIC(11,2)",
                  "STATUS",
                  "STATUS",
                  "BILLING_FREQ",
                  "STATUS")),
          table(
              "BILLING_INSTALLMENT_T",
              List.of("POLICY_ID", "INSTALLMENT_NO"),
              Map.of(
                  "INSTALLMENT_ID",
                  "SURROGATE",
                  "AMOUNT",
                  "NUMERIC(11,2)")),
          table(
              "RPT_RUN_LOG_T",
              List.of("PROGRAM_NAME", "STATUS"),
              Map.of("RUN_ID", "SURROGATE", "STATUS", "STATUS")));
      case "CLM006B" -> List.of(
          table(
              "CLAIM_RESERVE_T",
              List.of("CLAIM_ID", "RESERVE_ID"),
              Map.of(
                  "RESERVE_AMT",
                  "NUMERIC(11,2)",
                  "AUTHORITY_LIMIT",
                  "NUMERIC(11,2)",
                  "RESERVE_STATUS",
                  "STATUS")),
          table(
              "CLAIM_PAYMENT_T",
              List.of("CLAIM_ID", "PAYMENT_AMT"),
              Map.of(
                  "PAYMENT_ID",
                  "SURROGATE",
                  "PAYMENT_AMT",
                  "NUMERIC(11,2)",
                  "CREATED_AT",
                  "TIMESTAMP")),
          table(
              "RPT_RUN_LOG_T",
              List.of("PROGRAM_NAME", "STATUS"),
              Map.of("RUN_ID", "SURROGATE", "STATUS", "STATUS")));
      case "AUD002B" -> List.of(
          table(
              "AUDIT_LOG_T",
              List.of("PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"),
              Map.of("LOG_ID", "SURROGATE", "ACTION_CODE", "STATUS")),
          table(
              "AUDIT_LOG_ARCHIVE_T",
              List.of("PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"),
              Map.of("LOG_ID", "SURROGATE", "ARCHIVE_DATE", "TIMESTAMP")),
          table(
              "RPT_RUN_LOG_T",
              List.of("PROGRAM_NAME", "STATUS"),
              Map.of("RUN_ID", "SURROGATE", "STATUS", "STATUS")));
      case "CMM001B" -> List.of(
          table(
              "COMMISSION_T",
              List.of("POLICY_ID", "AGENT_ID"),
              Map.of(
                  "PREMIUM_AMT",
                  "NUMERIC(11,2)",
                  "COMMISSION_AMT",
                  "NUMERIC(9,2)",
                  "COMM_CALC_FLAG",
                  "STATUS")),
          table(
              "RPT_RUN_LOG_T",
              List.of("PROGRAM_NAME", "STATUS"),
              Map.of("RUN_ID", "SURROGATE", "STATUS", "STATUS")));
      case "POL006B" -> List.of(
          table("POLICY_T", List.of("POLICY_ID"), Map.of("STATUS", "STATUS")),
          table(
              "DEDUCTIBLE_T",
              List.of("POLICY_ID"),
              Map.of("DEDUCTIBLE_AMT", "NUMERIC(11,2)")),
          table(
              "RPT_RUN_LOG_T",
              List.of("PROGRAM_NAME", "STATUS"),
              Map.of("RUN_ID", "SURROGATE", "STATUS", "STATUS")));
      case "PRM005B" -> List.of(
          table("POLICY_T", List.of("POLICY_ID"), Map.of("STATUS", "STATUS")),
          table(
              "RPT_RUN_LOG_T",
              List.of("PROGRAM_NAME", "STATUS"),
              Map.of("RUN_ID", "SURROGATE", "STATUS", "STATUS")));
      default -> throw new ConfigurationException("Unknown program for default tables: " + program);
    };
  }

  private static TableDefinition table(
      String name, List<String> keys, Map<String, String> types) {
    return new TableDefinition(name, keys, new LinkedHashMap<>(types));
  }
}
