package com.pcis.customer.api.dto;

import com.pcis.customer.domain.CustomerAddressEntity;
import com.pcis.customer.domain.CustomerContactEntity;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.masking.MaskingService;
import org.springframework.stereotype.Component;

@Component
public class CustomerResponseMapper {

  private static final String CUSTOMER_ENTITY = "CUSTOMER_T";
  private static final String CONTACT_ENTITY = "CUSTOMER_CONTACT_T";

  private final MaskingService maskingService;

  public CustomerResponseMapper(MaskingService maskingService) {
    this.maskingService = maskingService;
  }

  public CustomerResponse toResponse(CustomerEntity entity) {
    return toResponse(entity, false);
  }

  public CustomerResponse toResponse(CustomerEntity entity, boolean unmasked) {
    return new CustomerResponse(
        entity.getCustId(),
        maskField(CUSTOMER_ENTITY, "TAX_ID", entity.getTaxId(), unmasked),
        entity.getCustName(),
        entity.getCustType(),
        entity.getCustStatus(),
        entity.getAddresses().stream().map(this::toAddress).toList(),
        entity.getContacts().stream().map(c -> toContact(c, unmasked)).toList());
  }

  private CustomerResponse.AddressResponse toAddress(CustomerAddressEntity address) {
    return new CustomerResponse.AddressResponse(
        address.getAddrId(),
        address.getAddressLine1(),
        address.getAddressLine2(),
        address.getCity(),
        address.getStateCode(),
        address.getZipCode(),
        address.getAddrType());
  }

  private CustomerResponse.ContactResponse toContact(
      CustomerContactEntity contact, boolean unmasked) {
    return new CustomerResponse.ContactResponse(
        contact.getContactId(),
        contact.getFirstName(),
        contact.getLastName(),
        maskField(CONTACT_ENTITY, "CONTACT_PHONE", contact.getPhoneNbr(), unmasked),
        maskField(CONTACT_ENTITY, "CONTACT_EMAIL", contact.getEmailAddr(), unmasked),
        contact.getContactType());
  }

  private String maskField(String entity, String column, String value, boolean unmasked) {
    if (unmasked || value == null) {
      return value;
    }
    return maskingService.mask(entity, column, value);
  }
}
