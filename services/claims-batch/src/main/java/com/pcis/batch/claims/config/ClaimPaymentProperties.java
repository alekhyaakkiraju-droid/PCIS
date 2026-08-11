package com.pcis.batch.claims.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.batch.claims")
public class ClaimPaymentProperties {

  private int chunkSize = 1;
  private int skipLimit = 100;
  private String programName = "CLM006B";
  private String batchServicePrincipal = "BATCH_SVC";
  private java.math.BigDecimal cessionThreshold = new java.math.BigDecimal("100000.00");

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getSkipLimit() {
    return skipLimit;
  }

  public void setSkipLimit(int skipLimit) {
    this.skipLimit = skipLimit;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public String getBatchServicePrincipal() {
    return batchServicePrincipal;
  }

  public void setBatchServicePrincipal(String batchServicePrincipal) {
    this.batchServicePrincipal = batchServicePrincipal;
  }

  public java.math.BigDecimal getCessionThreshold() {
    return cessionThreshold;
  }

  public void setCessionThreshold(java.math.BigDecimal cessionThreshold) {
    this.cessionThreshold = cessionThreshold;
  }
}
