package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.batch.bil003b.domain.BillingCandidateRow;
import com.pcis.billing.batch.bil003b.domain.BillingInstallmentDecision;
import com.pcis.billing.batch.bil003b.exception.BusinessRuleException;
import com.pcis.billing.batch.bil003b.infrastructure.BillingInstallmentProcessor;
import com.pcis.billing.config.BillingConfigProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillingInstallmentProcessorTest {

  private BillingInstallmentProcessor processor;

  @BeforeEach
  void setUp() {
    BillingGenerationProperties generationProperties = new BillingGenerationProperties();
    generationProperties.setReferenceDate(LocalDate.parse("2024-06-15"));
    BillingConfigProperties billingConfig = new BillingConfigProperties();
    billingConfig.setLeadDays(15);
    processor = new BillingInstallmentProcessor(generationProperties, billingConfig);
  }

  @Test
  void validCandidateProducesInstallmentDecision() {
    BillingCandidateRow candidate =
        new BillingCandidateRow(
            "POL001",
            new BigDecimal("600.00"),
            "M",
            12,
            1L,
            0,
            null);

    BillingInstallmentDecision decision = processor.process(candidate);

    assertThat(decision).isNotNull();
    assertThat(decision.installmentNbr()).isEqualTo(1);
    assertThat(decision.dueDate()).isEqualTo(LocalDate.parse("2024-06-15"));
    assertThat(decision.amount()).isEqualByComparingTo("50.00");
  }

  @Test
  void missingBillingPlanThrowsBusinessRuleException() {
    BillingCandidateRow candidate =
        new BillingCandidateRow(
            "POL002", new BigDecimal("600.00"), "M", 12, 0L, 0, null);

    assertThatThrownBy(() -> processor.process(candidate))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("billing plan");
  }

  @Test
  void negativePremiumThrowsBusinessRuleException() {
    BillingCandidateRow candidate =
        new BillingCandidateRow(
            "POL003", new BigDecimal("-1.00"), "M", 12, 1L, 0, null);

    assertThatThrownBy(() -> processor.process(candidate))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("premium");
  }

  @Test
  void outsideLeadWindowReturnsNull() {
    BillingCandidateRow candidate =
        new BillingCandidateRow(
            "POL004",
            new BigDecimal("600.00"),
            "M",
            12,
            1L,
            1,
            LocalDate.parse("2024-06-15"));

    assertThat(processor.process(candidate)).isNull();
  }
}
