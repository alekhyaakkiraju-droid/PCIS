package com.pcis.policy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "billing_plan")
public class BillingPlanEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "bill_plan_id", nullable = false)
  private Long billPlanId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pol_nbr", nullable = false, unique = true)
  private PolicyEntity policy;

  @Column(name = "bill_freq", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String billFreq;

  @Column(name = "nbr_installments", nullable = false)
  private Short nbrInstallments;

  @Column(name = "installment_fee", nullable = false, precision = 7, scale = 2)
  private BigDecimal installmentFee = BigDecimal.ZERO;

  @Column(name = "active_flag", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String activeFlag = "Y";

  public Long getBillPlanId() {
    return billPlanId;
  }

  public PolicyEntity getPolicy() {
    return policy;
  }

  public void setPolicy(PolicyEntity policy) {
    this.policy = policy;
  }

  public String getBillFreq() {
    return billFreq;
  }

  public void setBillFreq(String billFreq) {
    this.billFreq = billFreq;
  }

  public Short getNbrInstallments() {
    return nbrInstallments;
  }

  public void setNbrInstallments(Short nbrInstallments) {
    this.nbrInstallments = nbrInstallments;
  }

  public BigDecimal getInstallmentFee() {
    return installmentFee;
  }

  public void setInstallmentFee(BigDecimal installmentFee) {
    this.installmentFee = installmentFee;
  }

  public String getActiveFlag() {
    return activeFlag;
  }

  public void setActiveFlag(String activeFlag) {
    this.activeFlag = activeFlag;
  }
}
