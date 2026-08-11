package com.pcis.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;

@Entity
@Table(name = "claim")
public class ClaimEntity extends AuditableEntity {

  @Id
  @Column(name = "claim_nbr", nullable = false, length = 12)
  private String claimNbr;

  @Column(name = "pol_nbr", nullable = false, length = 12)
  private String polNbr;

  @Column(name = "cust_id", nullable = false)
  private Integer custId;

  @Column(name = "loss_date", nullable = false)
  private LocalDate lossDate;

  @Column(name = "claim_type", nullable = false, length = 3)
  private String claimType;

  @Column(name = "claim_status", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String claimStatus;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  public String getClaimNbr() { return claimNbr; }
  public void setClaimNbr(String claimNbr) { this.claimNbr = claimNbr; }

  public String getPolNbr() { return polNbr; }
  public void setPolNbr(String polNbr) { this.polNbr = polNbr; }

  public Integer getCustId() { return custId; }
  public void setCustId(Integer custId) { this.custId = custId; }

  public LocalDate getLossDate() { return lossDate; }
  public void setLossDate(LocalDate lossDate) { this.lossDate = lossDate; }

  public String getClaimType() { return claimType; }
  public void setClaimType(String claimType) { this.claimType = claimType; }

  public String getClaimStatus() { return claimStatus; }
  public void setClaimStatus(String claimStatus) { this.claimStatus = claimStatus; }

  public Long getVersion() { return version; }
}
