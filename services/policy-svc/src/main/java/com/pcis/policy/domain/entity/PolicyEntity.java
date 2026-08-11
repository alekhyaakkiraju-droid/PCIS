package com.pcis.policy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy")
public class PolicyEntity extends AuditableEntity {

  @Id
  @Column(name = "pol_nbr", nullable = false, length = 12)
  private String polNbr;

  @Column(name = "cust_id", nullable = false)
  private Integer custId;

  @Column(name = "agt_id", nullable = false, length = 8)
  private String agtId;

  @Column(name = "policy_type", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String policyType;

  @Column(name = "pol_status", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String polStatus;

  @Column(name = "eff_date", nullable = false)
  private LocalDate effDate;

  @Column(name = "exp_date", nullable = false)
  private LocalDate expDate;

  @Column(name = "prem_annual", nullable = false, precision = 13, scale = 2)
  private BigDecimal premAnnual;

  @Column(name = "renewal_of_pol", length = 12)
  private String renewalOfPol;

  @Column(name = "bill_freq", nullable = false, length = 1, columnDefinition = "char(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String billFreq;

  @OneToMany(mappedBy = "policy")
  private Set<CoverageEntity> coverages = new LinkedHashSet<>();

  @OneToMany(mappedBy = "policy")
  private Set<PolicyPropertyEntity> properties = new LinkedHashSet<>();

  @OneToMany(mappedBy = "policy")
  private Set<PolicyVehicleEntity> vehicles = new LinkedHashSet<>();

  @OneToMany(mappedBy = "policy")
  private Set<EndorsementEntity> endorsements = new LinkedHashSet<>();

  @OneToMany(mappedBy = "policy")
  private Set<PolicyHistoryEntity> history = new LinkedHashSet<>();

  public String getPolNbr() { return polNbr; }
  public void setPolNbr(String polNbr) { this.polNbr = polNbr; }
  public Integer getCustId() { return custId; }
  public void setCustId(Integer custId) { this.custId = custId; }
  public String getAgtId() { return agtId; }
  public void setAgtId(String agtId) { this.agtId = agtId; }
  public String getPolicyType() { return policyType; }
  public void setPolicyType(String policyType) { this.policyType = policyType; }
  public String getPolStatus() { return polStatus; }
  public void setPolStatus(String polStatus) { this.polStatus = polStatus; }
  public LocalDate getEffDate() { return effDate; }
  public void setEffDate(LocalDate effDate) { this.effDate = effDate; }
  public LocalDate getExpDate() { return expDate; }
  public void setExpDate(LocalDate expDate) { this.expDate = expDate; }
  public BigDecimal getPremAnnual() { return premAnnual; }
  public void setPremAnnual(BigDecimal premAnnual) { this.premAnnual = premAnnual; }
  public String getRenewalOfPol() { return renewalOfPol; }
  public void setRenewalOfPol(String renewalOfPol) { this.renewalOfPol = renewalOfPol; }
  public String getBillFreq() { return billFreq; }
  public void setBillFreq(String billFreq) { this.billFreq = billFreq; }
  public Set<CoverageEntity> getCoverages() { return coverages; }
  public Set<PolicyPropertyEntity> getProperties() { return properties; }
  public Set<PolicyVehicleEntity> getVehicles() { return vehicles; }
  public Set<EndorsementEntity> getEndorsements() { return endorsements; }
  public Set<PolicyHistoryEntity> getHistory() { return history; }
}
