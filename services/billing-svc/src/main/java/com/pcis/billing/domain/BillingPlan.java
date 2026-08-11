package com.pcis.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "BILLING_PLAN_T")
public class BillingPlan extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "BILL_PLAN_ID")
  private Long billPlanId;

  @Column(name = "POL_NBR", length = 12)
  private String polNbr;

  @Column(name = "BILL_FREQ", length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String billFreq;

  @Column(name = "INSTALLMENT_CNT")
  private Integer installmentCnt;

  @Column(name = "PLAN_STATUS", length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String planStatus;

  public Long getBillPlanId() {
    return billPlanId;
  }

  public void setBillPlanId(Long billPlanId) {
    this.billPlanId = billPlanId;
  }

  public String getPolNbr() {
    return polNbr;
  }

  public void setPolNbr(String polNbr) {
    this.polNbr = polNbr;
  }

  public String getBillFreq() {
    return billFreq;
  }

  public void setBillFreq(String billFreq) {
    this.billFreq = billFreq;
  }

  public Integer getInstallmentCnt() {
    return installmentCnt;
  }

  public void setInstallmentCnt(Integer installmentCnt) {
    this.installmentCnt = installmentCnt;
  }

  public String getPlanStatus() {
    return planStatus;
  }

  public void setPlanStatus(String planStatus) {
    this.planStatus = planStatus;
  }
}
