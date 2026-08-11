package com.pcis.customer.api.dto;

import com.pcis.customer.domain.model.UpdateCustomerCommand;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
    @Size(max = 11) String taxId,
    @Size(max = 60) String custName,
    @Size(max = 1) String custType,
    @Size(max = 1) String custStatus) {

  public UpdateCustomerCommand toCommand(Integer custId) {
    return new UpdateCustomerCommand(custId, taxId, custName, custType, custStatus);
  }
}
