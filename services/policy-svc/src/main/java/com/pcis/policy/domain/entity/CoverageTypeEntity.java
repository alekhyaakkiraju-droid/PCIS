package com.pcis.policy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "coverage_type")
public class CoverageTypeEntity extends AuditableEntity {

  @Id
  @Column(name = "cov_type", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String covType;

  @Column(name = "cov_desc", length = 60)
  private String covDesc;

  @Column(name = "active_flag", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String activeFlag;

  public String getCovType() { return covType; }
  public void setCovType(String covType) { this.covType = covType; }
  public String getCovDesc() { return covDesc; }
  public void setCovDesc(String covDesc) { this.covDesc = covDesc; }
  public String getActiveFlag() { return activeFlag; }
  public void setActiveFlag(String activeFlag) { this.activeFlag = activeFlag; }
}
