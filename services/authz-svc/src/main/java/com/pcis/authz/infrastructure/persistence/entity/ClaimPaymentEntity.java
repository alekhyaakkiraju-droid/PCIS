package com.pcis.authz.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "CLAIM_PAYMENT_T")
public class ClaimPaymentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "PAYMENT_ID", nullable = false)
  private Long paymentId;

  @Column(name = "CLAIM_ID", length = 12)
  private String claimId;

  @Column(name = "ADJUSTER_ID", length = 10)
  private String adjusterId;

  @Column(name = "PAYMENT_AMT", precision = 11, scale = 2)
  private BigDecimal paymentAmt;

  public Long getPaymentId() {
    return paymentId;
  }

  public String getClaimId() {
    return claimId;
  }

  public String getAdjusterId() {
    return adjusterId;
  }

  public BigDecimal getPaymentAmt() {
    return paymentAmt;
  }
}
