package com.pcis.billing.batch.prm005b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.prm005b.domain.DelinquencyDecisionEngine;
import com.pcis.billing.batch.prm005b.domain.StatusTransition;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DelinquencyDecisionEngineTest {

  @ParameterizedTest(name = "status={0} days={1} paid={2}/{3} grace={4} -> {5}")
  @CsvSource({
    "O, 0, 0, 500, 10, NONE",
    "O, 1, 0, 500, 10, NONE",
    "O, 10, 0, 500, 10, NONE",
    "O, 11, 0, 500, 10, L:true",
    "L, 30, 0, 500, 10, NONE",
    "O, 0, 500, 500, 10, P:false",
    "O, 11, 500, 500, 10, P:false",
    "O, 11, 499.99, 500, 10, L:true"
  })
  void decisionMatrix(
      String status, long daysPastDue, String amtPaid, String amtDue, int graceDays, String expected) {
    Optional<StatusTransition> result =
        DelinquencyDecisionEngine.evaluate(
            new BigDecimal(amtDue),
            new BigDecimal(amtPaid),
            daysPastDue,
            graceDays,
            status);

    if ("NONE".equals(expected)) {
      assertThat(result).isEmpty();
      return;
    }

    String[] parts = expected.split(":");
    String newStatus = parts[0];
    boolean incrementCounter = Boolean.parseBoolean(parts[1]);

    assertThat(result).isPresent();
    StatusTransition transition = result.get();
    assertThat(transition.oldStatus()).isEqualTo(status);
    assertThat(transition.newStatus()).isEqualTo(newStatus);
    assertThat(transition.daysPastDue()).isEqualTo(daysPastDue);
    assertThat(transition.incrementDelinquencyCounter()).isEqualTo(incrementCounter);
  }
}
