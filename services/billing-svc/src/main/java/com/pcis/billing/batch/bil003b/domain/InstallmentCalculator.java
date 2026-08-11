package com.pcis.billing.batch.bil003b.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure installment arithmetic with penny-remainder allocation on the last installment (BR-07). */
public final class InstallmentCalculator {

  private InstallmentCalculator() {}

  public static BigDecimal calculate(
      BigDecimal annualPremium, int installmentCount, int installmentNumber) {
    if (installmentCount <= 0) {
      throw new IllegalArgumentException("installmentCount must be positive");
    }
    if (installmentNumber <= 0 || installmentNumber > installmentCount) {
      throw new IllegalArgumentException("installmentNumber out of range");
    }
    BigDecimal premium = annualPremium == null ? BigDecimal.ZERO : annualPremium;
    if (installmentNumber == installmentCount) {
      BigDecimal unitAmount =
          premium.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
      return premium.subtract(unitAmount.multiply(BigDecimal.valueOf(installmentCount - 1L)));
    }
    return premium.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
  }

  public static BigDecimal sumInstallments(BigDecimal annualPremium, int installmentCount) {
    BigDecimal total = BigDecimal.ZERO;
    for (int i = 1; i <= installmentCount; i++) {
      total = total.add(calculate(annualPremium, installmentCount, i));
    }
    return total;
  }
}
