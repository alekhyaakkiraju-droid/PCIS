package com.pcis.premium.service;

import com.pcis.premium.infrastructure.TaxTableRepository;
import com.pcis.premium.infrastructure.TaxTableRepository.TaxRow;
import com.pcis.premium.model.PremiumDetailLine;
import com.pcis.premium.model.PremiumDetailLine.DetailLineType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaxService {

  private static final Logger log = LoggerFactory.getLogger(TaxService.class);
  private static final int MONEY_SCALE = 2;
  private static final int FACTOR_SCALE = 4;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private final TaxTableRepository taxTableRepository;

  public TaxService(TaxTableRepository taxTableRepository) {
    this.taxTableRepository = taxTableRepository;
  }

  public TaxResult calculateTaxes(
      BigDecimal taxableBase, String stateCode, LocalDate effectiveDate) {
    List<TaxRow> taxes = taxTableRepository.findEffectiveTaxes(stateCode, effectiveDate);
    if (taxes.isEmpty()) {
      log.warn("No tax rows for state={}; returning zero tax", stateCode);
      return new TaxResult(taxableBase, List.of());
    }

    BigDecimal originalBase = taxableBase;
    BigDecimal running = taxableBase;
    List<PremiumDetailLine> lines = new ArrayList<>();

    for (TaxRow tax : taxes) {
      if (tax.flatFee() != null && tax.flatFee().signum() != 0 && isFlatOnly(tax)) {
        continue;
      }
      BigDecimal base = tax.compound() ? running : originalBase;
      BigDecimal pct = tax.taxPct() == null ? BigDecimal.ZERO : tax.taxPct();
      BigDecimal taxAmount = base.multiply(pct).setScale(MONEY_SCALE, ROUNDING);
      running = running.add(taxAmount);
      String code = tax.taxType() == null ? "TAX" : tax.taxType();
      lines.add(
          new PremiumDetailLine(
              DetailLineType.TAX, code, code, pct.setScale(FACTOR_SCALE, ROUNDING), taxAmount));
    }

    for (TaxRow tax : taxes) {
      if (tax.flatFee() == null || tax.flatFee().signum() == 0) {
        continue;
      }
      BigDecimal flat = tax.flatFee().setScale(MONEY_SCALE, ROUNDING);
      running = running.add(flat);
      String code = (tax.taxType() == null ? "TAX" : tax.taxType()) + "-FEE";
      lines.add(new PremiumDetailLine(DetailLineType.TAX, code, code, BigDecimal.ONE, flat));
    }

    return new TaxResult(running.setScale(MONEY_SCALE, ROUNDING), List.copyOf(lines));
  }

  private static boolean isFlatOnly(TaxRow tax) {
    return tax.taxPct() == null || tax.taxPct().signum() == 0;
  }

  public record TaxResult(BigDecimal finalPremium, List<PremiumDetailLine> lines) {}
}
