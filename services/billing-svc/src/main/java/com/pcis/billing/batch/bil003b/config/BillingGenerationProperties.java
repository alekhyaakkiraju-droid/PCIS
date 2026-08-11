package com.pcis.billing.batch.bil003b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.billing.generation")
public class BillingGenerationProperties {

  private int chunkSize = 1;
  private int leadDays = 15;
  private String programName = "BIL003B";
  private java.time.LocalDate referenceDate = java.time.LocalDate.parse("2024-06-15");

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getLeadDays() {
    return leadDays;
  }

  public void setLeadDays(int leadDays) {
    this.leadDays = leadDays;
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
