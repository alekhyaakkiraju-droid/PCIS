package com.pcis.authz.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "CLAIM_ADJUSTER_T")
public class ClaimAdjusterEntity {

  @Id
  @Column(name = "ADJUSTER_ID", nullable = false, length = 10)
  private String adjusterId;

  @Column(name = "AUTHORITY_LIMIT", nullable = false, precision = 13, scale = 2)
  private BigDecimal authorityLimit;

  public String getAdjusterId() {
    return adjusterId;
  }

  public BigDecimal getAuthorityLimit() {
    return authorityLimit;
  }
}
