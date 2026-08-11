package com.pcis.customer.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_contact")
public class CustomerContactEntity extends AuditableEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "contact_id", nullable = false) private Long contactId;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cust_id", nullable = false) private CustomerEntity customer;
  @Column(name = "first_name", nullable = false, length = 30) private String firstName;
  @Column(name = "last_name", nullable = false, length = 30) private String lastName;
  @Column(name = "phone_nbr", length = 20) private String phoneNbr;
  @Column(name = "email_addr", length = 100) private String emailAddr;
  @Column(name = "contact_type", length = 3) private String contactType;
  public Long getContactId() { return contactId; }
  public void setContactId(Long contactId) { this.contactId = contactId; }
  public CustomerEntity getCustomer() { return customer; }
  public void setCustomer(CustomerEntity customer) { this.customer = customer; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getPhoneNbr() { return phoneNbr; }
  public void setPhoneNbr(String phoneNbr) { this.phoneNbr = phoneNbr; }
  public String getEmailAddr() { return emailAddr; }
  public void setEmailAddr(String emailAddr) { this.emailAddr = emailAddr; }
  public String getContactType() { return contactType; }
  public void setContactType(String contactType) { this.contactType = contactType; }
}
