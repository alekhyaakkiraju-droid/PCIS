package com.pcis.billing.batch.cmm001b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.billing.commission")
public class CommissionCalculationProperties {

  private int chunkSize = 1;
  private String programName = "CMM001B";
  private java.time.LocalDate referenceDate = java.time.LocalDate.parse("2024-06-15");

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

  public java.time.LocalDate getReferenceDate() {
    return referenceDate;
  }

  public void setReferenceDate(java.time.LocalDate referenceDate) {
    this.referenceDate = referenceDate;
  }
}
