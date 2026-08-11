package com.pcis.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "claim_adjuster")
public class ClaimAdjusterEntity extends AuditableEntity {

  @Id
  @Column(name = "adjuster_id", nullable = false, length = 10)
  private String adjusterId;

  @Column(name = "adjuster_name", nullable = false, length = 60)
  private String adjusterName;

  @Column(name = "authority_limit", nullable = false, precision = 11, scale = 2)
  private BigDecimal authorityLimit;

  public String getAdjusterId() { return adjusterId; }
  public void setAdjusterId(String adjusterId) { this.adjusterId = adjusterId; }

  public String getAdjusterName() { return adjusterName; }
  public void setAdjusterName(String adjusterName) { this.adjusterName = adjusterName; }

  public BigDecimal getAuthorityLimit() { return authorityLimit; }
  public void setAuthorityLimit(BigDecimal authorityLimit) { this.authorityLimit = authorityLimit; }
}
