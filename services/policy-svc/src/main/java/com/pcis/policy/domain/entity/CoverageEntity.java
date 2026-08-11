package com.pcis.policy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "coverage")
public class CoverageEntity extends AuditableEntity {

  @Id
  @Column(name = "coverage_id", nullable = false, length = 14)
  private String coverageId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pol_nbr", nullable = false)
  private PolicyEntity policy;

  @Column(name = "cov_type", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String covType;

  @Column(name = "limit_amt", nullable = false, precision = 13, scale = 2)
  private BigDecimal limitAmt;

  @Column(name = "ded_amt", nullable = false, precision = 11, scale = 2)
  private BigDecimal dedAmt;

  @Column(name = "cov_premium", nullable = false, precision = 13, scale = 2)
  private BigDecimal covPremium;

  @OneToMany(mappedBy = "coverage")
  private Set<DeductibleEntity> deductibles = new LinkedHashSet<>();

  public String getCoverageId() { return coverageId; }
  public void setCoverageId(String coverageId) { this.coverageId = coverageId; }
  public PolicyEntity getPolicy() { return policy; }
  public void setPolicy(PolicyEntity policy) { this.policy = policy; }
  public String getCovType() { return covType; }
  public void setCovType(String covType) { this.covType = covType; }
  public BigDecimal getLimitAmt() { return limitAmt; }
  public void setLimitAmt(BigDecimal limitAmt) { this.limitAmt = limitAmt; }
  public BigDecimal getDedAmt() { return dedAmt; }
  public void setDedAmt(BigDecimal dedAmt) { this.dedAmt = dedAmt; }
  public BigDecimal getCovPremium() { return covPremium; }
  public void setCovPremium(BigDecimal covPremium) { this.covPremium = covPremium; }
  public Set<DeductibleEntity> getDeductibles() { return deductibles; }
}
