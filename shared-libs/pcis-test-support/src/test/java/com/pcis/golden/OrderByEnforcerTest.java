package com.pcis.golden;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderByEnforcerTest {

  @Test
  void acceptsOrderByVariants() {
    assertDoesNotThrow(() -> OrderByEnforcer.requireOrderBy("select * from t order by a, b"));
    assertDoesNotThrow(() -> OrderByEnforcer.requireOrderBy("SELECT * FROM T ORDER BY A;"));
  }

  @Test
  void rejectsMissingOrBlank() {
    assertThrows(ConfigurationException.class, () -> OrderByEnforcer.requireOrderBy(""));
    assertThrows(
        ConfigurationException.class, () -> OrderByEnforcer.requireOrderBy("SELECT * FROM T"));
    assertThrows(
        ConfigurationException.class,
        () -> OrderByEnforcer.selectOrdered("T", List.of()));
  }

  @Test
  void buildsOrderedSelect() {
    String sql = OrderByEnforcer.selectOrdered("billing_installment_t", List.of("POLICY_ID", "INSTALLMENT_NO"));
    assertTrue(sql.contains("BILLING_INSTALLMENT_T"));
    assertTrue(sql.contains("ORDER BY POLICY_ID, INSTALLMENT_NO"));
  }
}
