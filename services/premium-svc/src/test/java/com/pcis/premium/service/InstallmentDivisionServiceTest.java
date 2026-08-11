package com.pcis.premium.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InstallmentDivisionServiceTest {

  private final InstallmentDivisionService service = new InstallmentDivisionService();

  @Test
  void divides1000By3WithRemainderOnFirstInstallment() {
    List<BigDecimal> installments = service.divideAnnualPremium(new BigDecimal("1000.00"), 3);
    assertThat(installments).containsExactly(
        new BigDecimal("333.34"), new BigDecimal("333.33"), new BigDecimal("333.33"));
    assertSumEquals("1000.00", installments);
  }

  @Test
  void divides100By12WithRemainderOnFirstInstallment() {
    List<BigDecimal> installments = service.divideAnnualPremium(new BigDecimal("100.00"), 12);
    assertThat(installments.getFirst()).isEqualByComparingTo("8.37");
    assertThat(installments.subList(1, 12))
        .allMatch(amount -> amount.compareTo(new BigDecimal("8.33")) == 0);
    assertSumEquals("100.00", installments);
  }

  @Test
  void dividesEvenlyWhenNoRemainder() {
    List<BigDecimal> installments = service.divideAnnualPremium(new BigDecimal("1200.00"), 12);
    assertThat(installments).hasSize(12);
    assertThat(installments).allMatch(amount -> amount.compareTo(new BigDecimal("100.00")) == 0);
    assertSumEquals("1200.00", installments);
  }

  @Test
  void singleInstallmentEqualsAnnualPremium() {
    List<BigDecimal> installments = service.divideAnnualPremium(new BigDecimal("500.00"), 1);
    assertThat(installments).containsExactly(new BigDecimal("500.00"));
  }

  @Test
  void pennyPremiumAcrossTwelveInstallments() {
    List<BigDecimal> installments = service.divideAnnualPremium(new BigDecimal("0.01"), 12);
    assertThat(installments.getFirst()).isEqualByComparingTo("0.01");
    assertThat(installments.subList(1, 12))
        .allMatch(amount -> amount.compareTo(BigDecimal.ZERO) == 0);
    assertSumEquals("0.01", installments);
  }

  @ParameterizedTest
  @CsvSource({
    "999999.99, 4",
    "250.55, 2",
    "100.00, 4",
    "100.00, 2",
    "100.00, 1"
  })
  void sumInvariantHolds(String premium, int count) {
    List<BigDecimal> installments =
        service.divideAnnualPremium(new BigDecimal(premium), count);
    assertSumEquals(premium, installments);
  }

  @Test
  void rejectsZeroPremium() {
    assertThatThrownBy(() -> service.divideAnnualPremium(BigDecimal.ZERO, 12))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("positive");
  }

  @Test
  void rejectsNegativePremium() {
    assertThatThrownBy(() -> service.divideAnnualPremium(new BigDecimal("-1.00"), 12))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsZeroInstallmentCount() {
    assertThatThrownBy(() -> service.divideAnnualPremium(new BigDecimal("100.00"), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("installmentCount");
  }

  private static void assertSumEquals(String expected, List<BigDecimal> installments) {
    BigDecimal sum = installments.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo(expected);
  }
}
