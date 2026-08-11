package com.pcis.customer.domain.exception;

import com.pcis.customer.domain.DuplicateCandidate;

public class DuplicateTaxIdException extends RuntimeException {

  public static final String REASON_CODE = "DUPLICATE_TAX_ID";

  private final String taxId;
  private final DuplicateCandidate existingCustomer;

  public DuplicateTaxIdException(String taxId, DuplicateCandidate existingCustomer) {
    super("Customer already exists with tax ID: " + taxId);
    this.taxId = taxId;
    this.existingCustomer = existingCustomer;
  }

  public String getTaxId() {
    return taxId;
  }

  public DuplicateCandidate getExistingCustomer() {
    return existingCustomer;
  }

  public String getReasonCode() {
    return REASON_CODE;
  }
}
