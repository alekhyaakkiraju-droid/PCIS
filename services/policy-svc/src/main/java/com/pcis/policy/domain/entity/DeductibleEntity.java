package com.pcis.policy.domain.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "deductible")
public class DeductibleEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deduct_id", nullable = false)
  private Long deductId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "coverage_id", nullable = false)
  private CoverageEntity coverage;

  @Column(name = "ded_amt", nullable = false, precision = 11, scale = 2)
  private BigDecimal dedAmt;

  @Column(name = "ded_type", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String dedType;

  public Long getDeductId() { return deductId; }
  public CoverageEntity getCoverage() { return coverage; }
  public void setCoverage(CoverageEntity coverage) { this.coverage = coverage; }
  public BigDecimal getDedAmt() { return dedAmt; }
  public void setDedAmt(BigDecimal dedAmt) { this.dedAmt = dedAmt; }
  public String getDedType() { return dedType; }
  public void setDedType(String dedType) { this.dedType = dedType; }
}
