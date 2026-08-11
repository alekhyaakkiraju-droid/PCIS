package com.pcis.golden;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.sql.DataSource;

/**
 * Cent-level golden output comparison engine. Loads golden JSON artifacts, captures actual
 * post-run database state, and compares with exact {@link BigDecimal} equality on monetary
 * columns.
 */
public final class GoldenComparator {

  private static final int DEFAULT_MAX_DIFF_ENTRIES = 100;
  private static final Set<String> MONETARY_TYPES =
      Set.of("NUMERIC(9,2)", "NUMERIC(11,2)");

  private final Path goldenRoot;
  private final NormalizationConfig normalizationConfig;
  private final int maxDiffEntries;
  private final GoldenFileLoader fileLoader;

  public GoldenComparator(Path goldenRoot, NormalizationConfig normalizationConfig) {
    this(goldenRoot, normalizationConfig, DEFAULT_MAX_DIFF_ENTRIES);
  }

  public GoldenComparator(
      Path goldenRoot, NormalizationConfig normalizationConfig, int maxDiffEntries) {
    this.goldenRoot = goldenRoot;
    this.normalizationConfig = normalizationConfig;
    this.maxDiffEntries = maxDiffEntries;
    this.fileLoader = new GoldenFileLoader(goldenRoot);
    normalizationConfig.validateBeforeComparison();
  }

  /** Default comparator using repo {@code golden/} root and classpath normalization rules. */
  public static GoldenComparator defaults() {
    return new GoldenComparator(resolveGoldenRoot(), NormalizationConfig.loadFromClasspath("normalization-rules.yaml"));
  }

  /**
   * JUnit 5 assertion entry point: loads golden for {@code scenarioId}, compares against
   * {@code actualDs}, throws {@link GoldenComparisonFailure} on mismatch.
   */
  public static void assertMatchesGolden(String scenarioId, DataSource actualDs) {
    defaults().assertMatches(scenarioId, actualDs);
  }

  public void assertMatches(String scenarioId, DataSource actualDs) {
    GoldenDiff diff = compare(scenarioId, actualDs);
    if (!diff.isMatch()) {
      throw new GoldenComparisonFailure(diff);
    }
  }

  /** Compare golden scenario against actual database state without asserting. */
  public GoldenDiff compare(String scenarioId, DataSource actualDs) {
    GoldenArtifact expected = fileLoader.load(scenarioId);
    GoldenArtifact actual = captureActual(scenarioId, actualDs, expected);
    return compareArtifacts(scenarioId, expected, actual);
  }

  /** Compare two in-memory artifacts (used by unit tests). */
  public GoldenDiff compareArtifacts(String scenarioId, GoldenArtifact expected, GoldenArtifact actual) {
    GoldenDiff.Builder builder = GoldenDiff.builder(scenarioId);

    CounterAssertion.compare(
        builder,
        expected.getRunLog(),
        actual.getRunLog(),
        CounterAssertion.findRunLogRow(expected),
        CounterAssertion.findRunLogRow(actual));

    Map<String, Map<String, Object>> expectedTables = indexTables(expected);
    Map<String, Map<String, Object>> actualTables = indexTables(actual);

    for (String tableName : unionKeys(expectedTables.keySet(), actualTables.keySet())) {
      Map<String, Object> expectedTable = expectedTables.get(tableName);
      Map<String, Object> actualTable = actualTables.get(tableName);
      if (expectedTable == null) {
        addExtraTableRows(builder, tableName, actualTable);
        continue;
      }
      if (actualTable == null) {
        addMissingTableRows(builder, tableName, expectedTable);
        continue;
      }
      compareTable(builder, tableName, expectedTable, actualTable);
    }

    return builder.build(maxDiffEntries);
  }

  private GoldenArtifact captureActual(
      String scenarioId, DataSource actualDs, GoldenArtifact expected) {
    String program = expected.getProgram();
    String referenceDate =
        expected.getReferenceDate() != null
            ? expected.getReferenceDate()
            : normalizationConfig.rules().defaultReferenceDate();
    GoldenCaptureContext context =
        GoldenCaptureContext.pinned(referenceDate, normalizationConfig.rules());
    GoldenOutputCapture capture = new GoldenOutputCapture(actualDs, context);
    List<TableDefinition> tables = tableDefinitionsFromGolden(expected);
    try {
      return capture.capture(
          program,
          expected.getScenario(),
          tables,
          expected.getDisplayOutput(),
          expected.getCompletionStatus());
    } catch (SQLException e) {
      throw new ConfigurationException("Failed to capture actual state for " + scenarioId, e);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<TableDefinition> tableDefinitionsFromGolden(GoldenArtifact artifact) {
    List<TableDefinition> defs = new ArrayList<>();
    for (Map<String, Object> table : artifact.getTables()) {
      String tableName = String.valueOf(table.get("tableName"));
      List<String> businessKeys = (List<String>) table.get("businessKeys");
      List<Map<String, String>> columns = (List<Map<String, String>>) table.get("columns");
      Map<String, String> types = new LinkedHashMap<>();
      if (columns != null) {
        for (Map<String, String> col : columns) {
          types.put(col.get("name"), col.get("type"));
        }
      }
      defs.add(new TableDefinition(tableName, businessKeys, types));
    }
    if (defs.isEmpty() && artifact.getProgram() != null) {
      return GoldenOutputCapture.defaultTablesFor(artifact.getProgram());
    }
    return defs;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Map<String, Object>> indexTables(GoldenArtifact artifact) {
    Map<String, Map<String, Object>> out = new TreeMap<>();
    for (Map<String, Object> table : artifact.getTables()) {
      out.put(String.valueOf(table.get("tableName")), table);
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private void compareTable(
      GoldenDiff.Builder builder,
      String tableName,
      Map<String, Object> expectedTable,
      Map<String, Object> actualTable) {
    List<String> businessKeys = (List<String>) expectedTable.get("businessKeys");
    Map<String, String> expectedTypes =
        columnTypes((List<Map<String, String>>) expectedTable.get("columns"));
    Map<String, String> actualTypes =
        columnTypes((List<Map<String, String>>) actualTable.get("columns"));

    List<Map<String, Object>> expectedRows = (List<Map<String, Object>>) expectedTable.get("rows");
    List<Map<String, Object>> actualRows = (List<Map<String, Object>>) actualTable.get("rows");
    if (expectedRows == null) {
      expectedRows = List.of();
    }
    if (actualRows == null) {
      actualRows = List.of();
    }

    Map<String, Map<String, Object>> expectedByKey = indexRows(expectedRows, businessKeys);
    Map<String, Map<String, Object>> actualByKey = indexRows(actualRows, businessKeys);

    for (String key : unionKeys(expectedByKey.keySet(), actualByKey.keySet())) {
      Map<String, Object> expectedRow = expectedByKey.get(key);
      Map<String, Object> actualRow = actualByKey.get(key);
      Map<String, Object> businessKey = parseBusinessKey(key, businessKeys, expectedRow, actualRow);

      if (expectedRow == null) {
        builder.add(
            new GoldenDiffEntry(
                tableName,
                businessKey,
                "*",
                "<present>",
                "<extra row>",
                null,
                DiffCategory.EXTRA_ROW));
        continue;
      }
      if (actualRow == null) {
        builder.add(
            new GoldenDiffEntry(
                tableName,
                businessKey,
                "*",
                "<present>",
                "<missing>",
                null,
                DiffCategory.MISSING_ROW));
        continue;
      }
      compareRow(builder, tableName, businessKey, expectedRow, actualRow, expectedTypes, actualTypes);
    }
  }

  private void compareRow(
      GoldenDiff.Builder builder,
      String tableName,
      Map<String, Object> businessKey,
      Map<String, Object> expectedRow,
      Map<String, Object> actualRow,
      Map<String, String> expectedTypes,
      Map<String, String> actualTypes) {
    for (String column : unionKeys(expectedRow.keySet(), actualRow.keySet())) {
      String expectedType = expectedTypes.getOrDefault(column, inferType(column));
      String actualType = actualTypes.getOrDefault(column, inferType(column));
      Object expectedVal = expectedRow.get(column);
      Object actualVal = actualRow.get(column);

      if (!expectedType.equals(actualType) && isTypedColumn(expectedType) && isTypedColumn(actualType)) {
        builder.add(
            new GoldenDiffEntry(
                tableName,
                businessKey,
                column,
                stringify(expectedVal),
                stringify(actualVal),
                null,
                DiffCategory.TYPE_MISMATCH));
        continue;
      }

      compareColumn(
          builder, tableName, businessKey, column, expectedType, actualType, expectedVal, actualVal);
    }
  }

  private void compareColumn(
      GoldenDiff.Builder builder,
      String tableName,
      Map<String, Object> businessKey,
      String column,
      String expectedType,
      String actualType,
      Object expectedVal,
      Object actualVal) {
    String type = expectedType != null ? expectedType : actualType;
    if (isMonetaryType(type)) {
      compareMonetary(builder, tableName, businessKey, column, expectedVal, actualVal);
      return;
    }
    if ("STATUS".equals(type) || normalizationConfig.rules().isDenied(column)) {
      compareStatus(builder, tableName, businessKey, column, expectedVal, actualVal);
      return;
    }
    if (normalizationConfig.rules().isAllowedNormalizeColumn(column)) {
      if (valuesEqualNormalized(expectedVal, actualVal)) {
        return;
      }
    }
    if (valuesEqual(expectedVal, actualVal, type)) {
      return;
    }
    builder.add(
        new GoldenDiffEntry(
            tableName,
            businessKey,
            column,
            stringify(expectedVal),
            stringify(actualVal),
            null,
            DiffCategory.VALUE_MISMATCH));
  }

  private void compareMonetary(
      GoldenDiff.Builder builder,
      String tableName,
      Map<String, Object> businessKey,
      String column,
      Object expectedVal,
      Object actualVal) {
    BigDecimal expected = toMoney(expectedVal);
    BigDecimal actual = toMoney(actualVal);

    if (expected == null && actual == null) {
      return;
    }
    if (expected == null || actual == null) {
      builder.add(
          new GoldenDiffEntry(
              tableName,
              businessKey,
              column,
              stringify(expectedVal),
              stringify(actualVal),
              null,
              DiffCategory.ONE_CENT_DIVERGENCE));
      return;
    }

    BigDecimal expectedNorm = normalizeMoneyScale(expected);
    BigDecimal actualNorm = normalizeMoneyScale(actual);
    if (expectedNorm.compareTo(actualNorm) == 0) {
      return;
    }

    BigDecimal delta = actualNorm.subtract(expectedNorm);
    builder.add(
        new GoldenDiffEntry(
            tableName,
            businessKey,
            column,
            expectedNorm.toPlainString(),
            actualNorm.toPlainString(),
            formatSignedDelta(delta),
            DiffCategory.ONE_CENT_DIVERGENCE));
  }

  private void compareStatus(
      GoldenDiff.Builder builder,
      String tableName,
      Map<String, Object> businessKey,
      String column,
      Object expectedVal,
      Object actualVal) {
    String expected = normalizeEmpty(expectedVal);
    String actual = normalizeEmpty(actualVal);
    if (expected.equals(actual)) {
      return;
    }
    builder.add(
        new GoldenDiffEntry(
            tableName,
            businessKey,
            column,
            expected,
            actual,
            null,
            DiffCategory.STATUS_MISMATCH));
  }

  @SuppressWarnings("unchecked")
  private void addMissingTableRows(
      GoldenDiff.Builder builder, String tableName, Map<String, Object> expectedTable) {
    List<Map<String, Object>> rows = (List<Map<String, Object>>) expectedTable.get("rows");
    List<String> businessKeys = (List<String>) expectedTable.get("businessKeys");
    if (rows == null) {
      return;
    }
    for (Map<String, Object> row : rows) {
      builder.add(
          new GoldenDiffEntry(
              tableName,
              extractKeyMap(row, businessKeys),
              "*",
              "<present>",
              "<missing>",
              null,
              DiffCategory.MISSING_ROW));
    }
  }

  @SuppressWarnings("unchecked")
  private void addExtraTableRows(
      GoldenDiff.Builder builder, String tableName, Map<String, Object> actualTable) {
    List<Map<String, Object>> rows = (List<Map<String, Object>>) actualTable.get("rows");
    List<String> businessKeys = (List<String>) actualTable.get("businessKeys");
    if (rows == null) {
      return;
    }
    for (Map<String, Object> row : rows) {
      builder.add(
          new GoldenDiffEntry(
              tableName,
              extractKeyMap(row, businessKeys),
              "*",
              "<present>",
              "<extra row>",
              null,
              DiffCategory.EXTRA_ROW));
    }
  }

  private static Map<String, String> columnTypes(List<Map<String, String>> columnMeta) {
    Map<String, String> types = new LinkedHashMap<>();
    if (columnMeta == null) {
      return types;
    }
    for (Map<String, String> col : columnMeta) {
      types.put(col.get("name"), col.get("type"));
    }
    return types;
  }

  private static Map<String, Map<String, Object>> indexRows(
      List<Map<String, Object>> rows, List<String> businessKeys) {
    Map<String, Map<String, Object>> out = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      out.put(buildRowKey(row, businessKeys), row);
    }
    return out;
  }

  private static String buildRowKey(Map<String, Object> row, List<String> businessKeys) {
    StringBuilder sb = new StringBuilder();
    for (String key : businessKeys) {
      if (sb.length() > 0) {
        sb.append('|');
      }
      sb.append(key).append('=').append(row.get(key));
    }
    return sb.toString();
  }

  private static Map<String, Object> parseBusinessKey(
      String encoded,
      List<String> businessKeys,
      Map<String, Object> expectedRow,
      Map<String, Object> actualRow) {
    Map<String, Object> key = new LinkedHashMap<>();
    Map<String, Object> source = expectedRow != null ? expectedRow : actualRow;
    if (source != null && businessKeys != null) {
      for (String bk : businessKeys) {
        key.put(bk, source.get(bk));
      }
      return key;
    }
    for (String part : encoded.split("\\|")) {
      int eq = part.indexOf('=');
      if (eq > 0) {
        key.put(part.substring(0, eq), part.substring(eq + 1));
      }
    }
    return key;
  }

  private static Map<String, Object> extractKeyMap(
      Map<String, Object> row, List<String> businessKeys) {
    Map<String, Object> key = new LinkedHashMap<>();
    if (businessKeys == null) {
      return key;
    }
    for (String bk : businessKeys) {
      key.put(bk, row.get(bk));
    }
    return key;
  }

  private static Iterable<String> unionKeys(Set<String> left, Set<String> right) {
    TreeMap<String, Boolean> keys = new TreeMap<>();
    left.forEach(k -> keys.put(k, Boolean.TRUE));
    right.forEach(k -> keys.put(k, Boolean.TRUE));
    return keys.keySet();
  }

  private static boolean isMonetaryType(String type) {
    return type != null && MONETARY_TYPES.contains(type.toUpperCase(Locale.ROOT));
  }

  private static boolean isTypedColumn(String type) {
    return type != null && !type.isBlank();
  }

  private static String inferType(String column) {
    return "STRING";
  }

  private static BigDecimal toMoney(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal bd) {
      return bd;
    }
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) {
      return null;
    }
    return new BigDecimal(s);
  }

  /** Normalize monetary scale so 100.0 and 100.00 compare equal without rounding tolerance. */
  static BigDecimal normalizeMoneyScale(BigDecimal value) {
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }

  private static String formatSignedDelta(BigDecimal delta) {
    if (delta.signum() > 0) {
      return "+" + delta.toPlainString();
    }
    return delta.toPlainString();
  }

  private static boolean valuesEqualNormalized(Object expected, Object actual) {
    return normalizeEmpty(expected).equals(normalizeEmpty(actual));
  }

  private static boolean valuesEqual(Object expected, Object actual, String type) {
    if ("INTEGER".equals(type)) {
      return integerEqual(expected, actual);
    }
    return normalizeEmpty(expected).equals(normalizeEmpty(actual));
  }

  private static boolean integerEqual(Object expected, Object actual) {
    if (expected == null && actual == null) {
      return true;
    }
    if (expected == null || actual == null) {
      return false;
    }
    return String.valueOf(expected).equals(String.valueOf(actual));
  }

  private static String normalizeEmpty(Object value) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value);
  }

  private static String stringify(Object value) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value);
  }

  static Path resolveGoldenRoot() {
    String env = System.getenv("PCIS_GOLDEN_ROOT");
    if (env != null && !env.isBlank()) {
      return Paths.get(env);
    }
    Path cwd = Paths.get("").toAbsolutePath();
    Path probe = cwd;
    for (int i = 0; i < 6; i++) {
      Path golden = probe.resolve("golden");
      if (Files.isDirectory(golden)) {
        return golden;
      }
      Path parent = probe.getParent();
      if (parent == null) {
        break;
      }
      probe = parent;
    }
    return cwd.resolve("golden");
  }

  public Path goldenRoot() {
    return goldenRoot;
  }

  public int maxDiffEntries() {
    return maxDiffEntries;
  }
}
