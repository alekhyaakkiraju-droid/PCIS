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
@Table(name = "recovery")
public class RecoveryEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "recovery_id", nullable = false)
  private Long recoveryId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_nbr", nullable = false)
  private ClaimEntity claim;

  @Column(name = "recovery_amt", nullable = false, precision = 11, scale = 2)
  private BigDecimal recoveryAmt;

  @Column(name = "recovery_type", nullable = false, length = 3)
  private String recoveryType;

  public Long getRecoveryId() { return recoveryId; }

  public ClaimEntity getClaim() { return claim; }
  public void setClaim(ClaimEntity claim) { this.claim = claim; }

  public BigDecimal getRecoveryAmt() { return recoveryAmt; }
  public void setRecoveryAmt(BigDecimal recoveryAmt) { this.recoveryAmt = recoveryAmt; }

  public String getRecoveryType() { return recoveryType; }
  public void setRecoveryType(String recoveryType) { this.recoveryType = recoveryType; }
}
