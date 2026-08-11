package com.pcis.customer.api.dto;

import java.util.List;

public record CustomerResponse(
    Integer custId,
    String taxId,
    String custName,
    String custType,
    String custStatus,
    List<AddressResponse> addresses,
    List<ContactResponse> contacts) {

  public record AddressResponse(
      Long addrId,
      String addressLine1,
      String addressLine2,
      String city,
      String stateCode,
      String zipCode,
      String addrType) {}

  public record ContactResponse(
      Long contactId,
      String firstName,
      String lastName,
      String phoneNbr,
      String emailAddr,
      String contactType) {}
}
