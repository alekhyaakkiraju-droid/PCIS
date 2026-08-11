package com.pcis.premium.service;

import com.pcis.premium.domain.MoneyAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Divides annual premium into installments with remainder allocated to the first installment
 * (PRD US-002). Legacy BIL003B omitted remainder handling, causing penny drift when installment
 * sums did not equal the annual premium.
 */
@Service
public class InstallmentDivisionService {

  private static final int SCALE = 2;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  public List<BigDecimal> divideAnnualPremium(BigDecimal annualPremium, int installmentCount) {
    validate(annualPremium, installmentCount);

    if (installmentCount == 1) {
      return List.of(scale(annualPremium));
    }

    MoneyAmount.DivisionResult division =
        MoneyAmount.of(annualPremium).divide(installmentCount);
    BigDecimal perInstallment = division.perUnit();
    BigDecimal remainder = division.remainder();
    BigDecimal firstInstallment = perInstallment.add(remainder);

    List<BigDecimal> installments = new ArrayList<>(installmentCount);
    installments.add(firstInstallment);
    for (int i = 1; i < installmentCount; i++) {
      installments.add(perInstallment);
    }

    assertSumInvariant(annualPremium, installments);
    return Collections.unmodifiableList(installments);
  }

  private static void validate(BigDecimal annualPremium, int installmentCount) {
    if (annualPremium == null || annualPremium.signum() <= 0) {
      throw new IllegalArgumentException(
          "annualPremium must be positive, got: " + annualPremium);
    }
    if (installmentCount <= 0) {
      throw new IllegalArgumentException(
          "installmentCount must be positive, got: " + installmentCount);
    }
  }

  private static BigDecimal scale(BigDecimal amount) {
    return amount.setScale(SCALE, ROUNDING);
  }

  private static void assertSumInvariant(
      BigDecimal annualPremium, List<BigDecimal> installments) {
    BigDecimal sum =
        installments.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, ROUNDING);
    if (sum.compareTo(scale(annualPremium)) != 0) {
      throw new IllegalStateException(
          "Installment sum invariant violated: expected "
              + annualPremium
              + " but got "
              + sum);
    }
  }
}
