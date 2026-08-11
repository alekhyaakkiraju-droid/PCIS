package com.pcis.customer.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "customer")
public class CustomerEntity extends AuditableEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cust_id", nullable = false)
  private Integer custId;
  @Column(name = "tax_id", length = 11) private String taxId;
  @Column(name = "cust_name", nullable = false, length = 60) private String custName;
  @Column(name = "cust_type", nullable = false, length = 1) private String custType;
  @Column(name = "cust_status", nullable = false, length = 1) private String custStatus = "A";
  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CustomerAddressEntity> addresses = new LinkedHashSet<>();
  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CustomerContactEntity> contacts = new LinkedHashSet<>();
  public Integer getCustId() { return custId; }
  public void setCustId(Integer custId) { this.custId = custId; }
  public String getTaxId() { return taxId; }
  public void setTaxId(String taxId) { this.taxId = taxId; }
  public String getCustName() { return custName; }
  public void setCustName(String custName) { this.custName = custName; }
  public String getCustType() { return custType; }
  public void setCustType(String custType) { this.custType = custType; }
  public String getCustStatus() { return custStatus; }
  public void setCustStatus(String custStatus) { this.custStatus = custStatus; }
  public Set<CustomerAddressEntity> getAddresses() { return addresses; }
  public Set<CustomerContactEntity> getContacts() { return contacts; }
  public void addAddress(CustomerAddressEntity address) { addresses.add(address); address.setCustomer(this); }
  public void addContact(CustomerContactEntity contact) { contacts.add(contact); contact.setCustomer(this); }
}
