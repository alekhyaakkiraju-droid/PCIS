package com.pcis.customer.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_address")
public class CustomerAddressEntity extends AuditableEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "addr_id", nullable = false) private Long addrId;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cust_id", nullable = false) private CustomerEntity customer;
  @Column(name = "address_line1", nullable = false, length = 40) private String addressLine1;
  @Column(name = "address_line2", length = 40) private String addressLine2;
  @Column(name = "city", nullable = false, length = 30) private String city;
  @Column(name = "state_code", nullable = false, length = 2) private String stateCode;
  @Column(name = "zip_code", nullable = false, length = 10) private String zipCode;
  @Column(name = "addr_type", nullable = false, length = 3) private String addrType = "PRM";
  public Long getAddrId() { return addrId; }
  public void setAddrId(Long addrId) { this.addrId = addrId; }
  public CustomerEntity getCustomer() { return customer; }
  public void setCustomer(CustomerEntity customer) { this.customer = customer; }
  public String getAddressLine1() { return addressLine1; }
  public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
  public String getAddressLine2() { return addressLine2; }
  public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getStateCode() { return stateCode; }
  public void setStateCode(String stateCode) { this.stateCode = stateCode; }
  public String getZipCode() { return zipCode; }
  public void setZipCode(String zipCode) { this.zipCode = zipCode; }
  public String getAddrType() { return addrType; }
  public void setAddrType(String addrType) { this.addrType = addrType; }
}
