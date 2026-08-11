package com.pcis.billing.batch.prm005b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.billing.delinquency")
public class DelinquencyAgingProperties {

  private int chunkSize = 1;
  private String programName = "PRM005B";
  private java.time.LocalDate referenceDate = java.time.LocalDate.parse("2024-06-15");
  /** Test-only: throw TemporaryException for this bill_sched_id when set. */
  private Long failBillSchedIdForTest;

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

  public Long getFailBillSchedIdForTest() {
    return failBillSchedIdForTest;
  }

  public void setFailBillSchedIdForTest(Long failBillSchedIdForTest) {
    this.failBillSchedIdForTest = failBillSchedIdForTest;
  }
}
