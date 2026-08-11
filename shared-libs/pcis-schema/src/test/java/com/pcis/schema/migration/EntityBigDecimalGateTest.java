package com.pcis.schema.migration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CI gate: {@code @Entity} mappings for monetary columns must use {@code BigDecimal} (WO-152).
 */
class EntityBigDecimalGateTest {

    private static List<MonetaryColumnSpec> monetaryColumns;
    private final EntityBigDecimalScanner scanner = new EntityBigDecimalScanner();

    @BeforeAll
    static void loadDictionary() throws Exception {
        monetaryColumns = MonetaryColumnSpecResolver.resolve(
                RepoPaths.dataDictionary(), RepoPaths.flywayBaselineSql());
    }

    @Test
    void entityMonetaryFieldsUseBigDecimal() throws Exception {
        List<EntityBigDecimalScanner.EntityFieldMapping> mappings =
                scanner.scanRepo(RepoPaths.findRepoRoot());
        List<EntityBigDecimalScanner.EntityCheckResult> results =
                scanner.validateMonetaryMappings(mappings, monetaryColumns);

        System.out.println("ENTITY BIGDECIMAL GATE REPORT");
        System.out.println("=============================");
        if (results.isEmpty()) {
            System.out.println("No @Entity monetary column mappings found yet — gate passes vacuously.");
        } else {
            for (EntityBigDecimalScanner.EntityCheckResult result : results) {
                System.out.println(result.formatLine());
            }
        }
        long failures = results.stream().filter(r -> !r.passed()).count();
        System.out.println("Summary: " + (results.size() - failures) + " PASS, " + failures + " FAIL");

        assertTrue(results.stream().allMatch(EntityBigDecimalScanner.EntityCheckResult::passed),
                () -> results.stream()
                        .filter(r -> !r.passed())
                        .map(EntityBigDecimalScanner.EntityCheckResult::formatLine)
                        .reduce((a, b) -> a + System.lineSeparator() + b)
                        .orElse("entity monetary mapping failures"));
    }

    @Test
    void rejectsDoubleForMonetaryColumn() {
        String badEntity = """
                package com.example;

                import jakarta.persistence.Column;
                import jakarta.persistence.Entity;
                import jakarta.persistence.Table;

                @Entity
                @Table(name = "policy_t")
                public class PolicyEntity {
                    @Column(name = "prem_annual")
                    private double premAnnual;
                }
                """;

        List<EntityBigDecimalScanner.EntityFieldMapping> mappings =
                scanner.scanSource(badEntity, "PolicyEntity.java");
        assertEquals(1, mappings.size());
        assertEquals("double", mappings.getFirst().javaType());

        MonetaryColumnSpec spec = monetaryColumns.stream()
                .filter(c -> "policy_t".equalsIgnoreCase(c.tableName())
                        && "prem_annual".equalsIgnoreCase(c.columnName()))
                .findFirst()
                .orElse(new MonetaryColumnSpec("POLICY_T", "prem_annual", 9, 2, MonetaryKind.AMOUNT));

        EntityBigDecimalScanner.EntityCheckResult result = scanner.validateMapping(mappings.getFirst(), spec);
        assertFalse(result.passed());
        assertTrue(result.message().contains("BigDecimal"));
    }

    @Test
    void acceptsBigDecimalForMonetaryColumn() {
        String goodEntity = """
                package com.example;

                import jakarta.persistence.Column;
                import jakarta.persistence.Entity;
                import jakarta.persistence.Table;
                import java.math.BigDecimal;

                @Entity
                @Table(name = "rate_factor_t")
                public class RateFactorEntity {
                    @Column(name = "factor_value")
                    private BigDecimal factorValue;
                }
                """;

        List<EntityBigDecimalScanner.EntityFieldMapping> mappings =
                scanner.scanSource(goodEntity, "RateFactorEntity.java");
        assertEquals(1, mappings.size());

        MonetaryColumnSpec spec = monetaryColumns.stream()
                .filter(c -> "rate_factor_t".equalsIgnoreCase(c.tableName())
                        && "factor_value".equalsIgnoreCase(c.columnName()))
                .findFirst()
                .orElse(new MonetaryColumnSpec("RATE_FACTOR_T", "factor_value", 7, 4, MonetaryKind.RATE_FACTOR));

        EntityBigDecimalScanner.EntityCheckResult result = scanner.validateMapping(mappings.getFirst(), spec);
        assertTrue(result.passed());
        assertTrue(result.message().contains("RATE_FACTOR"));
    }
}
