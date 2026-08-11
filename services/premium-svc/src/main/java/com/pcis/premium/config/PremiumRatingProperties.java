package com.pcis.premium.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcis.premium")
public class PremiumRatingProperties {

  @Min(1)
  @Max(3600)
  private int rateTableCacheTtlSeconds = 300;

  @DecimalMin("0.00")
  @DecimalMax("9999999.99")
  private BigDecimal referralThreshold = new BigDecimal("100000.00");

  @Min(0)
  @Max(4)
  private int decimalScale = 2;

  @Min(1)
  @Max(50)
  private int maxCoverageLinesPerRequest = 20;

  @Min(1)
  @Max(120)
  private int statementTimeoutSeconds = 30;

  @DecimalMin("0.0000")
  @DecimalMax("1.0000")
  private BigDecimal maxCombinedDiscountPct = new BigDecimal("0.2500");

  @DecimalMin("0.0000")
  @DecimalMax("1.0000")
  private BigDecimal maxCombinedSurchargePct = new BigDecimal("0.5000");

  @DecimalMin("0.00")
  private BigDecimal reinsuranceCessionThreshold = new BigDecimal("500000.00");

  public int getRateTableCacheTtlSeconds() {
    return rateTableCacheTtlSeconds;
  }

  public void setRateTableCacheTtlSeconds(int rateTableCacheTtlSeconds) {
    this.rateTableCacheTtlSeconds = rateTableCacheTtlSeconds;
  }

  public BigDecimal getReferralThreshold() {
    return referralThreshold;
  }

  public void setReferralThreshold(BigDecimal referralThreshold) {
    this.referralThreshold = referralThreshold;
  }

  public int getDecimalScale() {
    return decimalScale;
  }

  public void setDecimalScale(int decimalScale) {
    this.decimalScale = decimalScale;
  }

  public int getMaxCoverageLinesPerRequest() {
    return maxCoverageLinesPerRequest;
  }

  public void setMaxCoverageLinesPerRequest(int maxCoverageLinesPerRequest) {
    this.maxCoverageLinesPerRequest = maxCoverageLinesPerRequest;
  }

  public int getStatementTimeoutSeconds() {
    return statementTimeoutSeconds;
  }

  public void setStatementTimeoutSeconds(int statementTimeoutSeconds) {
    this.statementTimeoutSeconds = statementTimeoutSeconds;
  }

  public BigDecimal getMaxCombinedDiscountPct() {
    return maxCombinedDiscountPct;
  }

  public void setMaxCombinedDiscountPct(BigDecimal maxCombinedDiscountPct) {
    this.maxCombinedDiscountPct = maxCombinedDiscountPct;
  }

  public BigDecimal getMaxCombinedSurchargePct() {
    return maxCombinedSurchargePct;
  }

  public void setMaxCombinedSurchargePct(BigDecimal maxCombinedSurchargePct) {
    this.maxCombinedSurchargePct = maxCombinedSurchargePct;
  }

  public BigDecimal getReinsuranceCessionThreshold() {
    return reinsuranceCessionThreshold;
  }

  public void setReinsuranceCessionThreshold(BigDecimal reinsuranceCessionThreshold) {
    this.reinsuranceCessionThreshold = reinsuranceCessionThreshold;
  }
}
