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
import java.time.Instant;

@Entity
@Table(name = "approval")
public class ApprovalEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "approval_id", nullable = false)
  private Long approvalId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_nbr", nullable = false)
  private ClaimEntity claim;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reserve_id", nullable = false)
  private ClaimReserveEntity reserve;

  @Column(name = "approver_id", nullable = false, length = 10)
  private String approverId;

  @Column(name = "approval_status", nullable = false, length = 1)
  private String approvalStatus;

  @Column(name = "approval_date")
  private Instant approvalDate;

  public Long getApprovalId() { return approvalId; }

  public ClaimEntity getClaim() { return claim; }
  public void setClaim(ClaimEntity claim) { this.claim = claim; }

  public ClaimReserveEntity getReserve() { return reserve; }
  public void setReserve(ClaimReserveEntity reserve) { this.reserve = reserve; }

  public String getApproverId() { return approverId; }
  public void setApproverId(String approverId) { this.approverId = approverId; }

  public String getApprovalStatus() { return approvalStatus; }
  public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

  public Instant getApprovalDate() { return approvalDate; }
  public void setApprovalDate(Instant approvalDate) { this.approvalDate = approvalDate; }
}
