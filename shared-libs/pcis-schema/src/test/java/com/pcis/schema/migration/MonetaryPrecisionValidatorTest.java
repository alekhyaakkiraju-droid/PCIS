package com.pcis.schema.migration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MonetaryPrecisionValidator} with mock metadata (WO-152).
 */
class MonetaryPrecisionValidatorTest {

    private static List<MonetaryColumnSpec> monetaryColumns;
    private final MonetaryPrecisionValidator validator = new MonetaryPrecisionValidator();

    @BeforeAll
    static void loadDictionary() throws Exception {
        monetaryColumns = DataDictionaryMonetaryLoader.load(RepoPaths.dataDictionary());
    }

    @Test
    void resolvesMonetarySpecsFromDictionaryAndFlyway() throws Exception {
        List<MonetaryColumnSpec> specs = MonetaryColumnSpecResolver.resolve(
                RepoPaths.dataDictionary(), RepoPaths.flywayBaselineSql());

        assertTrue(specs.size() >= 30);
        MonetaryColumnSpec billingDue = specs.stream()
                .filter(s -> "billing_schedule_t".equalsIgnoreCase(s.tableName())
                        && "amt_due".equalsIgnoreCase(s.columnName()))
                .findFirst()
                .orElseThrow();
        assertEquals(11, billingDue.precision());
        assertEquals(2, billingDue.scale());
        assertEquals(MonetaryKind.AMOUNT, billingDue.kind());

        MonetaryColumnSpec policyPremium = specs.stream()
                .filter(s -> "policy_t".equalsIgnoreCase(s.tableName())
                        && "prem_annual".equalsIgnoreCase(s.columnName()))
                .findFirst()
                .orElseThrow();
        assertEquals(13, policyPremium.precision());
        assertEquals(MonetaryKind.AMOUNT, policyPremium.kind());
    }

    @Test
    void passesWhenMetadataMatchesDictionary() {
        MonetaryColumnSpec spec = new MonetaryColumnSpec("POLICY_T", "prem_annual", 13, 2, MonetaryKind.AMOUNT);
        ColumnMetadataProvider provider = MonetaryPrecisionValidator.fromMap(Map.of(
                "policy_t.prem_annual", new ColumnMetadata("numeric", 13, 2)));

        ColumnCheckResult result = validator.validateOne(spec, provider);
        assertTrue(result.passed());
        assertEquals("PASS", result.status());

        MonetaryPrecisionValidator.MonetaryPrecisionReport report =
                validator.buildReport(List.of(result));
        assertTrue(report.format().contains("policy_t.prem_annual: PASS"));
    }

    @Test
    void failsOnDoublePrecisionColumn() {
        MonetaryColumnSpec spec = new MonetaryColumnSpec("POLICY_T", "prem_annual", 13, 2, MonetaryKind.AMOUNT);
        ColumnMetadataProvider provider = MonetaryPrecisionValidator.fromMap(Map.of(
                "policy_t.prem_annual", new ColumnMetadata("double precision", null, null)));

        ColumnCheckResult result = validator.validateOne(spec, provider);
        assertFalse(result.passed());
        assertEquals("FAIL", result.status());
        assertTrue(result.message().contains("forbidden"));

        MonetaryPrecisionValidator.MonetaryPrecisionReport report =
                validator.buildReport(List.of(result));
        System.out.println(report.format());
        assertTrue(report.format().contains("policy_t.prem_annual: FAIL"));
        assertTrue(report.format().contains("Summary: 0 PASS, 1 FAIL"));
    }

    @Test
    void failsOnFloat4Column() {
        MonetaryColumnSpec spec = new MonetaryColumnSpec("CLAIM_RESERVE_T", "reserve_amt", 13, 2, MonetaryKind.AMOUNT);
        ColumnMetadataProvider provider = MonetaryPrecisionValidator.fromMap(Map.of(
                "claim_reserve_t.reserve_amt", new ColumnMetadata("real", null, null)));

        ColumnCheckResult result = validator.validateOne(spec, provider);
        assertFalse(result.passed());
        assertEquals("real", result.actualDataType());
    }

    @Test
    void failsOnPrecisionMismatch() {
        MonetaryColumnSpec spec = new MonetaryColumnSpec("BILLING_SCHEDULE_T", "amt_due", 11, 2, MonetaryKind.AMOUNT);
        ColumnMetadataProvider provider = MonetaryPrecisionValidator.fromMap(Map.of(
                "billing_schedule_t.amt_due", new ColumnMetadata("numeric", 11, 4)));

        ColumnCheckResult result = validator.validateOne(spec, provider);
        assertFalse(result.passed());
        assertTrue(result.message().contains("precision/scale mismatch"));
    }

    @Test
    void reportSummarizesMixedResults() {
        MonetaryColumnSpec passSpec = new MonetaryColumnSpec("RATE_FACTOR_T", "factor_value", 7, 4, MonetaryKind.RATE_FACTOR);
        MonetaryColumnSpec failSpec = new MonetaryColumnSpec("COMMISSION_LEDGER_T", "commission_amt", 11, 2, MonetaryKind.AMOUNT);

        ColumnMetadataProvider provider = MonetaryPrecisionValidator.fromMap(Map.of(
                "rate_factor_t.factor_value", new ColumnMetadata("numeric", 7, 4),
                "commission_ledger_t.commission_amt", new ColumnMetadata("money", null, null)));

        List<ColumnCheckResult> results = List.of(
                validator.validateOne(passSpec, provider),
                validator.validateOne(failSpec, provider));

        MonetaryPrecisionValidator.MonetaryPrecisionReport report = validator.buildReport(results);
        String formatted = report.format();
        System.out.println(formatted);

        assertEquals(1, report.passCount());
        assertEquals(1, report.failCount());
        assertTrue(formatted.contains("RATE_FACTOR"));
        assertTrue(formatted.contains("Summary: 1 PASS, 1 FAIL"));
    }
}
