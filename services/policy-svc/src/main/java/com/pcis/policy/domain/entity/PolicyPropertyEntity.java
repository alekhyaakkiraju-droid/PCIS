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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy_property")
public class PolicyPropertyEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "property_id", nullable = false)
  private Long propertyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pol_nbr", nullable = false)
  private PolicyEntity policy;

  @Column(name = "prop_type", nullable = false, length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String propType;

  @Column(name = "addr_line1", nullable = false, length = 50)
  private String addrLine1;

  @Column(name = "state_code", nullable = false, length = 2, columnDefinition = "char(2)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String stateCode;

  public Long getPropertyId() { return propertyId; }
  public PolicyEntity getPolicy() { return policy; }
  public void setPolicy(PolicyEntity policy) { this.policy = policy; }
  public String getPropType() { return propType; }
  public void setPropType(String propType) { this.propType = propType; }
  public String getAddrLine1() { return addrLine1; }
  public void setAddrLine1(String addrLine1) { this.addrLine1 = addrLine1; }
  public String getStateCode() { return stateCode; }
  public void setStateCode(String stateCode) { this.stateCode = stateCode; }
}
