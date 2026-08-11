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
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "endorsement")
public class EndorsementEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "endorse_id", nullable = false)
  private Long endorseId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pol_nbr", nullable = false)
  private PolicyEntity policy;

  @Column(name = "end_type", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String endType;

  @Column(name = "eff_date", nullable = false)
  private LocalDate effDate;

  @Column(name = "prem_chg", nullable = false, precision = 11, scale = 2)
  private BigDecimal premChg;

  public Long getEndorseId() { return endorseId; }
  public PolicyEntity getPolicy() { return policy; }
  public void setPolicy(PolicyEntity policy) { this.policy = policy; }
  public String getEndType() { return endType; }
  public void setEndType(String endType) { this.endType = endType; }
  public LocalDate getEffDate() { return effDate; }
  public void setEffDate(LocalDate effDate) { this.effDate = effDate; }
  public BigDecimal getPremChg() { return premChg; }
  public void setPremChg(BigDecimal premChg) { this.premChg = premChg; }
}
