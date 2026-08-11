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
import java.math.BigDecimal;

@Entity
@Table(name = "claim_reserve")
public class ClaimReserveEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reserve_id", nullable = false)
  private Long reserveId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_nbr", nullable = false)
  private ClaimEntity claim;

  @Column(name = "reserve_type", nullable = false, length = 3)
  private String reserveType;

  @Column(name = "approved_amt", nullable = false, precision = 11, scale = 2)
  private BigDecimal approvedAmt;

  @Column(name = "paid_to_date", nullable = false, precision = 11, scale = 2)
  private BigDecimal paidToDate;

  @Column(name = "reserve_status", nullable = false, length = 1)
  private String reserveStatus;

  public Long getReserveId() { return reserveId; }

  public ClaimEntity getClaim() { return claim; }
  public void setClaim(ClaimEntity claim) { this.claim = claim; }

  public String getReserveType() { return reserveType; }
  public void setReserveType(String reserveType) { this.reserveType = reserveType; }

  public BigDecimal getApprovedAmt() { return approvedAmt; }
  public void setApprovedAmt(BigDecimal approvedAmt) { this.approvedAmt = approvedAmt; }

  public BigDecimal getPaidToDate() { return paidToDate; }
  public void setPaidToDate(BigDecimal paidToDate) { this.paidToDate = paidToDate; }

  public String getReserveStatus() { return reserveStatus; }
  public void setReserveStatus(String reserveStatus) { this.reserveStatus = reserveStatus; }
}
