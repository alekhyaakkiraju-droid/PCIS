package com.pcis.customer.domain.exception;

public class CustomerNotFoundException extends RuntimeException {
  private final Integer custId;
  public CustomerNotFoundException(Integer custId) { super("Customer not found: " + custId); this.custId = custId; }
  public Integer getCustId() { return custId; }
}
