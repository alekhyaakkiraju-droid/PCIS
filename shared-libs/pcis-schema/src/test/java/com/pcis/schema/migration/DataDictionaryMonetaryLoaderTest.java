package com.pcis.schema.migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataDictionaryMonetaryLoaderTest {

    @Test
    void distinguishesAmountAndRateFactorScales() throws Exception {
        List<MonetaryColumnSpec> specs = DataDictionaryMonetaryLoader.load(RepoPaths.dataDictionary());

        assertTrue(specs.stream().anyMatch(s -> s.kind() == MonetaryKind.AMOUNT && s.scale() == 2));
        assertTrue(specs.stream().anyMatch(s -> s.kind() == MonetaryKind.RATE_FACTOR && s.scale() == 4));

        MonetaryColumnSpec baseRate = specs.stream()
                .filter(s -> "base_rate".equalsIgnoreCase(s.columnName()))
                .findFirst()
                .orElseThrow();
        assertEquals(MonetaryKind.RATE_FACTOR, baseRate.kind());
        assertEquals(4, baseRate.scale());
    }
}
