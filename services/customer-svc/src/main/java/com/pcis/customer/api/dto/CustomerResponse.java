package com.pcis.customer.api.dto;

import com.pcis.customer.domain.CustomerEntity;

public record CustomerResponse(
    Integer custId, String taxId, String custName, String custType, String custStatus) {

  public static CustomerResponse from(CustomerEntity entity) {
    return new CustomerResponse(
        entity.getCustId(),
        entity.getTaxId(),
        entity.getCustName(),
        entity.getCustType(),
        entity.getCustStatus());
  }
}
