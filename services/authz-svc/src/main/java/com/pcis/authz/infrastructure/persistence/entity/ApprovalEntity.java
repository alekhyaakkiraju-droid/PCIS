package com.pcis.authz.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "APPROVAL_T")
public class ApprovalEntity {

  public static final String STATUS_APPROVED = "APPROVED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "APPROVAL_ID", nullable = false)
  private Long approvalId;

  @Column(name = "CLAIM_ID", length = 12)
  private String claimId;

  @Column(name = "RESERVE_HIST_ID")
  private Long reserveHistId;

  @Column(name = "APPROVER_ID", length = 10)
  private String approverId;

  @Column(name = "APPROVAL_STATUS", length = 10)
  private String approvalStatus;

  public Long getApprovalId() {
    return approvalId;
  }

  public String getClaimId() {
    return claimId;
  }

  public Long getReserveHistId() {
    return reserveHistId;
  }

  public String getApproverId() {
    return approverId;
  }

  public String getApprovalStatus() {
    return approvalStatus;
  }
}
