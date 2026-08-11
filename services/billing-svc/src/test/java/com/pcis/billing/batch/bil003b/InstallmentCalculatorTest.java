package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.bil003b.domain.InstallmentCalculator;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InstallmentCalculatorTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("installmentCases")
  void calculateReturnsExpectedAmount(
      String scenario,
      BigDecimal premium,
      int count,
      int installmentNumber,
      BigDecimal expected) {
    assertThat(InstallmentCalculator.calculate(premium, count, installmentNumber))
        .isEqualByComparingTo(expected);
  }

  @ParameterizedTest(name = "sum equals premium for {0}")
  @MethodSource("sumCases")
  void sumOfInstallmentsEqualsAnnualPremium(String scenario, BigDecimal premium, int count) {
    assertThat(InstallmentCalculator.sumInstallments(premium, count))
        .isEqualByComparingTo(premium);
  }

  static Stream<Arguments> installmentCases() {
    return Stream.of(
        Arguments.of(
            "$1000/3 installment 1",
            new BigDecimal("1000.00"),
            3,
            1,
            new BigDecimal("333.33")),
        Arguments.of(
            "$1000/3 installment 2",
            new BigDecimal("1000.00"),
            3,
            2,
            new BigDecimal("333.33")),
        Arguments.of(
            "$1000/3 installment 3 (remainder)",
            new BigDecimal("1000.00"),
            3,
            3,
            new BigDecimal("333.34")),
        Arguments.of(
            "$100/1 single",
            new BigDecimal("100.00"),
            1,
            1,
            new BigDecimal("100.00")),
        Arguments.of(
            "$0.01/2 installment 1",
            new BigDecimal("0.01"),
            2,
            1,
            new BigDecimal("0.01")),
        Arguments.of(
            "$0.01/2 installment 2",
            new BigDecimal("0.01"),
            2,
            2,
            new BigDecimal("0.00")),
        Arguments.of(
            "$0.00/1 zero premium",
            new BigDecimal("0.00"),
            1,
            1,
            new BigDecimal("0.00")));
  }

  static Stream<Arguments> sumCases() {
    return Stream.of(
        Arguments.of(Named.of("$1000/3", "penny remainder"), new BigDecimal("1000.00"), 3),
        Arguments.of(Named.of("$500/12", "twelve installments"), new BigDecimal("500.00"), 12),
        Arguments.of(Named.of("$999.99/7", "seven-way split"), new BigDecimal("999.99"), 7),
        Arguments.of(Named.of("$0.01/2", "sub-penny"), new BigDecimal("0.01"), 2));
  }
}
