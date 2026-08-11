package com.pcis.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "claim_payment")
public class ClaimPaymentEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_id", nullable = false)
  private Long paymentId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_nbr", nullable = false)
  private ClaimEntity claim;

  @Column(name = "payment_amt", nullable = false, precision = 11, scale = 2)
  private BigDecimal paymentAmt;

  @Column(name = "payment_status", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String paymentStatus;

  @Column(name = "payee_id")
  private Integer payeeId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approval_id")
  private ApprovalEntity approval;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "adjuster_id")
  private ClaimAdjusterEntity adjuster;

  public Long getPaymentId() { return paymentId; }

  public ClaimEntity getClaim() { return claim; }
  public void setClaim(ClaimEntity claim) { this.claim = claim; }

  public BigDecimal getPaymentAmt() { return paymentAmt; }
  public void setPaymentAmt(BigDecimal paymentAmt) { this.paymentAmt = paymentAmt; }

  public String getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

  public Integer getPayeeId() { return payeeId; }
  public void setPayeeId(Integer payeeId) { this.payeeId = payeeId; }

  public ApprovalEntity getApproval() { return approval; }
  public void setApproval(ApprovalEntity approval) { this.approval = approval; }

  public ClaimAdjusterEntity getAdjuster() { return adjuster; }
  public void setAdjuster(ClaimAdjusterEntity adjuster) { this.adjuster = adjuster; }
}
