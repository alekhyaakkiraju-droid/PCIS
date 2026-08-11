package com.pcis.customer.api.dto;

import com.pcis.customer.domain.model.CreateCustomerCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCustomerRequest(
    @Size(max = 11) String taxId,
    @NotBlank @Size(max = 60) String custName,
    @NotBlank @Size(max = 1) String custType,
    List<AddressRequest> addresses,
    List<ContactRequest> contacts) {

  public CreateCustomerRequest {
    addresses = addresses == null ? List.of() : List.copyOf(addresses);
    contacts = contacts == null ? List.of() : List.copyOf(contacts);
  }

  public CreateCustomerCommand toCommand() {
    return new CreateCustomerCommand(
        taxId,
        custName,
        custType,
        addresses.stream().map(AddressRequest::toCommand).toList(),
        contacts.stream().map(ContactRequest::toCommand).toList());
  }

  public record AddressRequest(
      @NotBlank @Size(max = 40) String addressLine1,
      @Size(max = 40) String addressLine2,
      @NotBlank @Size(max = 30) String city,
      @NotBlank @Size(max = 2) String stateCode,
      @NotBlank @Size(max = 10) String zipCode,
      @Size(max = 3) String addrType) {

    CreateCustomerCommand.AddressCommand toCommand() {
      return new CreateCustomerCommand.AddressCommand(
          addressLine1, addressLine2, city, stateCode, zipCode, addrType);
    }
  }

  public record ContactRequest(
      @NotBlank @Size(max = 30) String firstName,
      @NotBlank @Size(max = 30) String lastName,
      @Size(max = 20) String phoneNbr,
      @Size(max = 100) String emailAddr,
      @Size(max = 3) String contactType) {

    CreateCustomerCommand.ContactCommand toCommand() {
      return new CreateCustomerCommand.ContactCommand(
          firstName, lastName, phoneNbr, emailAddr, contactType);
    }
  }
}
