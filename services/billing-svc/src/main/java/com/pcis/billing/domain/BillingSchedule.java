package com.pcis.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "BILLING_SCHEDULE_T")
public class BillingSchedule extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "BILL_SCHED_ID")
  private Long billSchedId;

  @Column(name = "POL_NBR", nullable = false, length = 12)
  private String polNbr;

  @Column(name = "BILL_PLAN_ID", nullable = false)
  private Long billPlanId;

  @Column(name = "INSTALLMENT_NBR", nullable = false)
  private Integer installmentNbr;

  @Column(name = "DUE_DATE", nullable = false)
  private LocalDate dueDate;

  @Column(name = "AMT_DUE", nullable = false, precision = 11, scale = 2)
  private BigDecimal amtDue;

  @Column(name = "AMT_PAID", precision = 11, scale = 2)
  private BigDecimal amtPaid;

  @Column(name = "SCHED_STATUS", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String schedStatus;

  @Column(name = "COMM_CALC_FLAG", length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String commCalcFlag;

  @Column(name = "REC_DELINQUENT")
  private Integer recDelinquent;

  public Long getBillSchedId() {
    return billSchedId;
  }

  public void setBillSchedId(Long billSchedId) {
    this.billSchedId = billSchedId;
  }

  public String getPolNbr() {
    return polNbr;
  }

  public void setPolNbr(String polNbr) {
    this.polNbr = polNbr;
  }

  public Long getBillPlanId() {
    return billPlanId;
  }

  public void setBillPlanId(Long billPlanId) {
    this.billPlanId = billPlanId;
  }

  public Integer getInstallmentNbr() {
    return installmentNbr;
  }

  public void setInstallmentNbr(Integer installmentNbr) {
    this.installmentNbr = installmentNbr;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public BigDecimal getAmtDue() {
    return amtDue;
  }

  public void setAmtDue(BigDecimal amtDue) {
    this.amtDue = amtDue;
  }

  public BigDecimal getAmtPaid() {
    return amtPaid;
  }

  public void setAmtPaid(BigDecimal amtPaid) {
    this.amtPaid = amtPaid;
  }

  public String getSchedStatus() {
    return schedStatus;
  }

  public void setSchedStatus(String schedStatus) {
    this.schedStatus = schedStatus;
  }

  public String getCommCalcFlag() {
    return commCalcFlag;
  }

  public void setCommCalcFlag(String commCalcFlag) {
    this.commCalcFlag = commCalcFlag;
  }

  public Integer getRecDelinquent() {
    return recDelinquent;
  }

  public void setRecDelinquent(Integer recDelinquent) {
    this.recDelinquent = recDelinquent;
  }
}
