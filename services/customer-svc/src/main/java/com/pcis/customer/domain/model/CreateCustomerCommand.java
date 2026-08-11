package com.pcis.customer.domain.model;

import java.util.List;

public record CreateCustomerCommand(String taxId, String custName, String custType, List<AddressCommand> addresses, List<ContactCommand> contacts) {
  public record AddressCommand(String addressLine1, String addressLine2, String city, String stateCode, String zipCode, String addrType) {}
  public record ContactCommand(String firstName, String lastName, String phoneNbr, String emailAddr, String contactType) {}
}
