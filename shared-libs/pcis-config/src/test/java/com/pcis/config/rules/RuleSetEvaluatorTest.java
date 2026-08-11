package com.pcis.config.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pcis.config.PcisCodeTableProperties;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleSetEvaluatorTest {

  private RuleSetRepository repository;
  private RuleSetEvaluator evaluator;

  @BeforeEach
  void setUp() {
    repository = mock(RuleSetRepository.class);
    evaluator = new RuleSetEvaluator(repository, new PcisCodeTableProperties());
  }

  @Test
  void resolvesBillingFrequencyIntervalRuleSet() {
    when(repository.findByKeyAndVersion("billing-frequency-interval", 1))
        .thenReturn(billingRuleSetRow());

    BillingFrequencyIntervalRuleSet ruleSet = evaluator.billingFrequencyIntervalRuleSet();

    assertThat(ruleSet.intervalMonthsByFrequency()).containsEntry("Q", 3);
    assertThat(ruleSet.defaultIntervalMonths()).isEqualTo(12);
  }

  @Test
  void unknownFrequencyFallsBackToDefaultWithObservableFlag() {
    when(repository.findByKeyAndVersion("billing-frequency-interval", 1))
        .thenReturn(billingRuleSetRow());

    RuleSetEvaluator.IntervalResolution resolution = evaluator.resolveBillingIntervalMonths("X");

    assertThat(resolution.intervalMonths()).isEqualTo(12);
    assertThat(resolution.usedFallback()).isTrue();
  }

  @Test
  void knownFrequencyDoesNotUseFallback() {
    when(repository.findByKeyAndVersion("billing-frequency-interval", 1))
        .thenReturn(billingRuleSetRow());

    RuleSetEvaluator.IntervalResolution resolution = evaluator.resolveBillingIntervalMonths("S");

    assertThat(resolution.intervalMonths()).isEqualTo(6);
    assertThat(resolution.usedFallback()).isFalse();
  }

  @Test
  void ruleSetViewsAreImmutable() {
    when(repository.findByKeyAndVersion("billing-frequency-interval", 1))
        .thenReturn(billingRuleSetRow());

    BillingFrequencyIntervalRuleSet first = evaluator.billingFrequencyIntervalRuleSet();
    BillingFrequencyIntervalRuleSet second = evaluator.billingFrequencyIntervalRuleSet();

    assertThat(first).isSameAs(second);
    assertThatThrownBy(() -> first.intervalMonthsByFrequency().put("Z", 99))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static RuleSetRow billingRuleSetRow() {
    return new RuleSetRow(
        "billing-frequency-interval",
        1,
        """
        {
          "mappings": [
            {"frequency": "M", "intervalMonths": 1},
            {"frequency": "Q", "intervalMonths": 3},
            {"frequency": "S", "intervalMonths": 6},
            {"frequency": "A", "intervalMonths": 12}
          ],
          "defaultIntervalMonths": 12
        }
        """,
        LocalDate.now().minusDays(1),
        null,
        "A");
  }
}
