package com.pcis.batch.claims.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.claims.payment")
public class ClaimPaymentProperties {

  private int chunkSize = 1;
  private String programName = "CLM006B";
  private java.math.BigDecimal cessionThreshold = new java.math.BigDecimal("100000.00");

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public java.math.BigDecimal getCessionThreshold() {
    return cessionThreshold;
  }

  public void setCessionThreshold(java.math.BigDecimal cessionThreshold) {
    this.cessionThreshold = cessionThreshold;
  }
}
