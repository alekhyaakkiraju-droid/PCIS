package com.pcis.customer.domain.exception;

public class DuplicateTaxIdException extends RuntimeException {
  private final String taxId;
  public DuplicateTaxIdException(String taxId) { super("Customer already exists with tax ID: " + taxId); this.taxId = taxId; }
  public String getTaxId() { return taxId; }
}
