package com.pcis.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoldenCaptureContextTest {

  private NormalizationRules rules;
  private GoldenCaptureContext ctx;

  @BeforeEach
  void setUp() {
    rules = NormalizationRules.loadFromClasspath("normalization-rules.yaml");
    ctx = GoldenCaptureContext.pinned("2024-06-15", rules);
  }

  @Test
  void datePinningReturnsFixedReferenceDate() {
    assertEquals(LocalDate.of(2024, 6, 15), ctx.currentDate());
    assertEquals(LocalDate.of(2024, 6, 15), ctx.referenceDate());
    assertEquals(
        LocalDate.of(2024, 6, 15).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
        ctx.now());
  }

  @Test
  void sequenceNormalizationIsStableAcrossOffsetChanges() {
    assertEquals("SEQ_001", ctx.sequenceNormalizer().normalize("PAYMENT_ID", 9001));
    assertEquals("SEQ_002", ctx.sequenceNormalizer().normalize("PAYMENT_ID", 9002));
    // Same original → same ordinal
    assertEquals("SEQ_001", ctx.sequenceNormalizer().normalize("PAYMENT_ID", 9001));

    GoldenCaptureContext other = GoldenCaptureContext.pinned("2024-06-15", rules);
    assertEquals("SEQ_001", other.sequenceNormalizer().normalize("PAYMENT_ID", 42));
    assertEquals("SEQ_002", other.sequenceNormalizer().normalize("PAYMENT_ID", 99));
  }

  @Test
  void timestampsNormalizeButMonetaryAndStatusDoNot() {
    assertEquals(
        GoldenCaptureContext.NORMALIZED_TS,
        ctx.normalizeValue("CREATED_AT", "TIMESTAMP", "2024-06-15 14:22:33"));
    assertEquals(
        "SEQ_001", ctx.normalizeValue("PAYMENT_ID", "SURROGATE", 10042));

    assertEquals(
        "1500.00",
        ctx.normalizeValue("PAYMENT_AMT", "NUMERIC(11,2)", new BigDecimal("1500.00")));
    assertEquals("AP", ctx.normalizeValue("RESERVE_STATUS", "STATUS", "AP"));
    assertEquals("A", ctx.normalizeValue("STATUS", "STATUS", "A"));
    assertEquals(
        "100.00",
        ctx.normalizeValue("COMMISSION_AMT", "NUMERIC(9,2)", new BigDecimal("100.00")));
  }

  @Test
  void orderByEnforcementRejectsUnorderedCaptureSql() {
    assertTrue(OrderByEnforcer.hasOrderBy("SELECT * FROM T ORDER BY A"));
    assertFalse(OrderByEnforcer.hasOrderBy("SELECT * FROM T"));
    assertThrows(
        ConfigurationException.class, () -> OrderByEnforcer.requireOrderBy("SELECT * FROM T"));
    String sql = OrderByEnforcer.selectOrdered("POLICY_T", java.util.List.of("POLICY_ID"));
    assertTrue(sql.contains("ORDER BY POLICY_ID"));
  }

  @Test
  void fromRulesDefaultUsesYamlReferenceDate() {
    GoldenCaptureContext def = GoldenCaptureContext.fromRulesDefault(rules);
    assertEquals(LocalDate.of(2024, 6, 15), def.referenceDate());
  }
}
