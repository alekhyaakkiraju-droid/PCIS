package com.pcis.authz.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "CLAIM_RESERVE_T")
public class ClaimReserveEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "RESERVE_HIST_ID", nullable = false)
  private Long reserveHistId;

  @Column(name = "CLAIM_ID", nullable = false, length = 12)
  private String claimId;

  @Column(name = "PAID_TO_DATE", nullable = false, precision = 13, scale = 2)
  private BigDecimal paidToDate;

  public Long getReserveHistId() {
    return reserveHistId;
  }

  public String getClaimId() {
    return claimId;
  }

  public BigDecimal getPaidToDate() {
    return paidToDate;
  }
}
