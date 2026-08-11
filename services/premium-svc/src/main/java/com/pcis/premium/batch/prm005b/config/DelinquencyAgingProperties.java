package com.pcis.premium.batch.prm005b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.premium.delinquency")
public class DelinquencyAgingProperties {

  private int chunkSize = 1;
  private int graceDays = 10;
  private String programName = "PRM005B";
  private java.time.LocalDate referenceDate = java.time.LocalDate.parse("2024-06-15");

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getGraceDays() {
    return graceDays;
  }

  public void setGraceDays(int graceDays) {
    this.graceDays = graceDays;
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
