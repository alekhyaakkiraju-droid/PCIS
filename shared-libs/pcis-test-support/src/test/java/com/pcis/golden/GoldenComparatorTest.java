package com.pcis.golden;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoldenComparatorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private GoldenComparator comparator;
  private GoldenFileLoader loader;
  private Path goldenRoot;

  @BeforeEach
  void setUp() {
    goldenRoot = GoldenComparator.resolveGoldenRoot();
    NormalizationConfig config = NormalizationConfig.loadFromClasspath("normalization-rules.yaml");
    comparator = new GoldenComparator(goldenRoot, config, 100);
    loader = new GoldenFileLoader(goldenRoot);
  }

  @Test
  void equalStatesPass() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenDiff diff = comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, expected);
    assertTrue(diff.isMatch());
    assertEquals(0, diff.getTotalDiffCount());
    assertDoesNotThrow(() -> assertMatches("test-comparator-fixtures/pass-case", expected, expected));
  }

  @Test
  void oneCentDivergenceFailsWithDelta() throws Exception {
    GoldenArtifact expected = loadFixture("one-cent-off-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, "100.01");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/one-cent-off-case", expected, actual);
    assertFalse(diff.isMatch());
    assertEquals(1, diff.getTotalDiffCount());
    GoldenDiffEntry entry = diff.getEntries().get(0);
    assertEquals(DiffCategory.ONE_CENT_DIVERGENCE, entry.getCategory());
    assertEquals("BILLING_INSTALLMENT_T", entry.getTable());
    assertEquals("AMOUNT", entry.getColumn());
    assertEquals("100.00", entry.getExpectedValue());
    assertEquals("100.01", entry.getActualValue());
    assertEquals("+0.01", entry.getSignedDelta());
    assertEquals("POLTEST001", entry.getBusinessKey().get("POLICY_ID"));
    assertEquals(1, entry.getBusinessKey().get("INSTALLMENT_NO"));
  }

  @Test
  void missingRowDetected() throws Exception {
    GoldenArtifact expected = loadFixture("missing-row-case");
    GoldenArtifact actual = deepCopy(expected);
    removeInstallment(actual, "POLTEST001", 2);

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/missing-row-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.MISSING_ROW));
  }

  @Test
  void extraRowDetected() throws Exception {
    GoldenArtifact expected = loadFixture("extra-row-case");
    GoldenArtifact actual = deepCopy(expected);
    addInstallment(actual, "POLTEST001", 2, "100.00", "SEQ_002");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/extra-row-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.EXTRA_ROW));
  }

  @Test
  void columnTypeMismatchDetected() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setColumnType(actual, "BILLING_INSTALLMENT_T", "AMOUNT", "STRING");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.TYPE_MISMATCH));
  }

  @Test
  void scaleMismatchDoesNotFalseFail() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, "100.0");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertTrue(diff.isMatch(), () -> GoldenDiffTextWriter.render(diff));
  }

  @Test
  void nullMonetaryNotEqualToZero() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, null);

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.ONE_CENT_DIVERGENCE));
  }

  @Test
  void nullVsEmptyStringHandling() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setStringColumn(actual, "BILLING_INSTALLMENT_T", "POLTEST001", 1, "INSTALLMENT_ID", "");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    // Surrogate columns are normalization-allowed; SEQ_001 vs empty may differ
    assertFalse(diff.isMatch());
  }

  @Test
  void rowOrderingWithSameBusinessKeysPasses() throws Exception {
    GoldenArtifact expected = loadFixture("missing-row-case");
    GoldenArtifact actual = deepCopy(expected);
    reverseInstallmentRows(actual);

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/missing-row-case", expected, actual);
    assertTrue(diff.isMatch());
  }

  @Test
  void statusMismatchDetected() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setRunLogStatus(actual, "FAILED");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.STATUS_MISMATCH));
  }

  @Test
  void counterMismatchDetected() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setRowsProcessed(actual, 99);

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.COUNTER_MISMATCH));
  }

  @Test
  void oversizedDiffTruncationPreservesTotalCount() throws Exception {
    GoldenComparator smallLimit = new GoldenComparator(goldenRoot, NormalizationConfig.loadFromClasspath("normalization-rules.yaml"), 3);
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    for (int i = 1; i <= 10; i++) {
      addInstallment(actual, "POLTEST001", i + 1, "100.00", "SEQ_" + String.format("%03d", i + 1));
    }

    GoldenDiff diff =
        smallLimit.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(diff.getTotalDiffCount() >= 10);
    assertTrue(diff.isTruncated());
    assertEquals(3, diff.getEntries().size());
  }

  @Test
  void normalizationConfigRejectionThrowsBeforeComparison() {
    NormalizationRules badRules =
        NormalizationRules.fromYaml(
            new StringReader(
            """
            version: 1
            allow:
              timestamps: [CREATED_AT]
              surrogates: [PAYMENT_ID, AMOUNT]
            deny:
              monetary_types: ["NUMERIC(9,2)", "NUMERIC(11,2)"]
              monetary_columns: [AMOUNT]
              status_columns: [STATUS]
              status_suffixes: [_STATUS]
            business_keys: {}
            default_reference_date: "2024-06-15"
            """));
    assertThrows(ConfigurationValidationException.class, () -> NormalizationConfig.fromRules(badRules));
  }

  @Test
  void proposedMonetaryAllowListRejected() {
    NormalizationConfig config = NormalizationConfig.loadFromClasspath("normalization-rules.yaml");
    assertThrows(
        ConfigurationValidationException.class,
        () -> config.validateProposedAllowList(List.of("AMOUNT", "PAYMENT_AMT")));
  }

  @Test
  void jsonAndTextWritersProduceOutput() throws Exception {
    GoldenArtifact expected = loadFixture("one-cent-off-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, "100.01");
    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/one-cent-off-case", expected, actual);

    String json = GoldenDiffJsonWriter.render(diff);
    assertTrue(json.contains("ONE_CENT_DIVERGENCE"));
    assertTrue(json.contains("100.01"));

    String text = GoldenDiffTextWriter.render(diff);
    assertTrue(text.contains("BILLING_INSTALLMENT_T"));
    assertTrue(text.contains("+0.01"));
  }

  @Test
  void assertMatchesGoldenThrowsGoldenComparisonFailure() throws Exception {
    GoldenArtifact expected = loadFixture("one-cent-off-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, "100.01");

    GoldenComparisonFailure failure =
        assertThrows(
            GoldenComparisonFailure.class,
            () -> assertMatches("test-comparator-fixtures/one-cent-off-case", expected, actual));
    assertEquals(1, failure.getDiff().getTotalDiffCount());
  }

  @Test
  void zeroRowsInBothTablesDoesNotFalseMissingRow() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    clearTableRows(expected, "BILLING_INSTALLMENT_T");
    clearTableRows(actual, "BILLING_INSTALLMENT_T");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertTrue(diff.isMatch());
  }

  @Test
  void valueMismatchDetectedForStringColumn() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    setStringColumn(actual, "BILLING_INSTALLMENT_T", "POLTEST001", 1, "NOTES", "mutated");

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.VALUE_MISMATCH));
  }

  @Test
  void wholeTableMissingDetected() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    actual.getTables().removeIf(t -> "BILLING_INSTALLMENT_T".equals(t.get("tableName")));

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.MISSING_ROW));
  }

  @Test
  void wholeTableExtraDetected() throws Exception {
    GoldenArtifact expected = loadFixture("pass-case");
    GoldenArtifact actual = deepCopy(expected);
    Map<String, Object> extraTable = new LinkedHashMap<>();
    extraTable.put("tableName", "EXTRA_TABLE_T");
    extraTable.put("businessKeys", List.of("ID"));
    extraTable.put("columns", List.of(Map.of("name", "ID", "type", "INTEGER")));
    extraTable.put("rows", List.of(Map.of("ID", 1)));
    actual.getTables().add(extraTable);

    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/pass-case", expected, actual);
    assertFalse(diff.isMatch());
    assertTrue(
        diff.getEntries().stream().anyMatch(e -> e.getCategory() == DiffCategory.EXTRA_ROW));
  }

  @Test
  void missingGoldenFileThrowsGoldenFileNotFoundException() {
    assertThrows(
        GoldenFileNotFoundException.class,
        () -> loader.load("test-comparator-fixtures/does-not-exist"));
  }

  @Test
  void invalidScenarioIdThrowsConfigurationException() {
    assertThrows(ConfigurationException.class, () -> loader.load("invalid-scenario-id"));
  }

  @Test
  void staticAssertMatchesGoldenWithDataSource() throws Exception {
    // Exercises the static entry point wiring; full DB path covered in integration tests.
    assertDoesNotThrow(GoldenComparator::defaults);
  }

  @Test
  void jsonWriterWritesToFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
    GoldenArtifact expected = loadFixture("one-cent-off-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, "100.01");
    GoldenDiff diff =
        comparator.compareArtifacts("test-comparator-fixtures/one-cent-off-case", expected, actual);
    Path out = dir.resolve("diff.json");
    GoldenDiffJsonWriter.write(diff, out);
    assertTrue(java.nio.file.Files.readString(out).contains("ONE_CENT_DIVERGENCE"));
  }

  @Test
  void goldenComparisonFailureIncludesDiff() throws Exception {
    GoldenArtifact expected = loadFixture("one-cent-off-case");
    GoldenArtifact actual = deepCopy(expected);
    setAmount(actual, "POLTEST001", 1, "100.01");
    GoldenComparisonFailure failure =
        assertThrows(
            GoldenComparisonFailure.class,
            () -> assertMatches("test-comparator-fixtures/one-cent-off-case", expected, actual));
    assertFalse(failure.getDiff().isMatch());
    assertTrue(failure.getMessage().contains("ONE_CENT_DIVERGENCE"));
  }

  @Test
  void loadNormalizationConfigFromGoldenRoot() {
    Path rules = goldenRoot.resolve("normalization-rules.yaml");
    if (java.nio.file.Files.isRegularFile(rules)) {
      assertDoesNotThrow(() -> NormalizationConfig.load(rules));
    }
  }

  @Test
  void normalizeMoneyScaleHandlesEquivalentValues() {
    assertEquals(
        0,
        GoldenComparator.normalizeMoneyScale(new BigDecimal("100.0"))
            .compareTo(GoldenComparator.normalizeMoneyScale(new BigDecimal("100.00"))));
  }

  @Test
  void goldenFileLoaderResolvesTestFixtures() {
    GoldenArtifact artifact = loader.load("test-comparator-fixtures/pass-case");
    assertEquals("TESTFIX", artifact.getProgram());
    assertEquals("pass-case", artifact.getScenario());
  }

  private GoldenArtifact loadFixture(String scenario) throws Exception {
    return loader.load("test-comparator-fixtures/" + scenario);
  }

  private GoldenArtifact deepCopy(GoldenArtifact source) throws Exception {
    return MAPPER.readValue(MAPPER.writeValueAsBytes(source), GoldenArtifact.class);
  }

  @SuppressWarnings("unchecked")
  private void setAmount(GoldenArtifact artifact, String policyId, int installmentNo, String amount) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!"BILLING_INSTALLMENT_T".equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
      for (Map<String, Object> row : rows) {
        if (policyId.equals(row.get("POLICY_ID")) && installmentNo == (Integer) row.get("INSTALLMENT_NO")) {
          if (amount == null) {
            row.put("AMOUNT", null);
          } else {
            row.put("AMOUNT", amount);
          }
          return;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void removeInstallment(GoldenArtifact artifact, String policyId, int installmentNo) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!"BILLING_INSTALLMENT_T".equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
      rows.removeIf(
          row ->
              policyId.equals(row.get("POLICY_ID"))
                  && installmentNo == (Integer) row.get("INSTALLMENT_NO"));
    }
  }

  @SuppressWarnings("unchecked")
  private void addInstallment(
      GoldenArtifact artifact, String policyId, int installmentNo, String amount, String surrogate) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!"BILLING_INSTALLMENT_T".equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("POLICY_ID", policyId);
      row.put("INSTALLMENT_NO", installmentNo);
      row.put("AMOUNT", amount);
      row.put("INSTALLMENT_ID", surrogate);
      rows.add(row);
    }
  }

  @SuppressWarnings("unchecked")
  private void reverseInstallmentRows(GoldenArtifact artifact) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!"BILLING_INSTALLMENT_T".equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
      List<Map<String, Object>> copy = new ArrayList<>(rows);
      rows.clear();
      for (int i = copy.size() - 1; i >= 0; i--) {
        rows.add(copy.get(i));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setColumnType(GoldenArtifact artifact, String tableName, String column, String type) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!tableName.equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, String>> columns = (List<Map<String, String>>) table.get("columns");
      for (Map<String, String> col : columns) {
        if (column.equals(col.get("name"))) {
          col.put("type", type);
          return;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setStringColumn(
      GoldenArtifact artifact,
      String tableName,
      String policyId,
      int installmentNo,
      String column,
      String value) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (!tableName.equals(table.get("tableName"))) {
        continue;
      }
      List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
      for (Map<String, Object> row : rows) {
        if (policyId.equals(row.get("POLICY_ID")) && installmentNo == (Integer) row.get("INSTALLMENT_NO")) {
          row.put(column, value);
          return;
        }
      }
    }
  }

  private void setRunLogStatus(GoldenArtifact artifact, String status) {
    artifact.getRunLog().put("status", status);
    for (Map<String, Object> table : artifact.getTables()) {
      if ("RPT_RUN_LOG_T".equals(table.get("tableName"))) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
        if (!rows.isEmpty()) {
          rows.get(0).put("STATUS", status);
        }
      }
    }
  }

  private void setRowsProcessed(GoldenArtifact artifact, int count) {
    artifact.getRunLog().put("rowsProcessed", count);
    for (Map<String, Object> table : artifact.getTables()) {
      if ("RPT_RUN_LOG_T".equals(table.get("tableName"))) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
        if (!rows.isEmpty()) {
          rows.get(0).put("ROWS_PROCESSED", count);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void clearTableRows(GoldenArtifact artifact, String tableName) {
    for (Map<String, Object> table : artifact.getTables()) {
      if (tableName.equals(table.get("tableName"))) {
        ((List<Map<String, Object>>) table.get("rows")).clear();
      }
    }
  }

  /** Package-private overload for unit tests without DataSource. */
  private void assertMatches(String scenarioId, GoldenArtifact expected, GoldenArtifact actual) {
    GoldenDiff diff = comparator.compareArtifacts(scenarioId, expected, actual);
    if (!diff.isMatch()) {
      throw new GoldenComparisonFailure(diff);
    }
  }
}
