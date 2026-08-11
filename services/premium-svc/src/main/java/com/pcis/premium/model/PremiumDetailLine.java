package com.pcis.premium.model;

import java.math.BigDecimal;
import java.util.List;

public record PremiumDetailLine(
    DetailLineType lineType, String code, String description, BigDecimal factor, BigDecimal amount) {

  public enum DetailLineType {
    RISK,
    DISCOUNT,
    SURCHARGE,
    TAX,
    FACTOR
  }
}
