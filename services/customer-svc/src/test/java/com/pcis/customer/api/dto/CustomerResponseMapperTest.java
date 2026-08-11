package com.pcis.customer.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pcis.customer.domain.CustomerContactEntity;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.masking.MaskingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerResponseMapperTest {

  @Mock private MaskingService maskingService;

  @InjectMocks private CustomerResponseMapper mapper;

  @Test
  void masksTaxIdEmailAndPhoneByDefault() {
    CustomerEntity customer = new CustomerEntity();
    customer.setCustId(1);
    customer.setTaxId("123456789");
    customer.setCustName("Jane Doe");
    customer.setCustType("I");
    customer.setCustStatus("A");

    CustomerContactEntity contact = new CustomerContactEntity();
    contact.setContactId(9L);
    contact.setFirstName("Jane");
    contact.setLastName("Doe");
    contact.setPhoneNbr("2145550199");
    contact.setEmailAddr("jane@example.com");
    contact.setContactType("PRM");
    customer.getContacts().add(contact);

    when(maskingService.mask("CUSTOMER_T", "TAX_ID", "123456789")).thenReturn("*****6789");
    when(maskingService.mask("CUSTOMER_CONTACT_T", "CONTACT_PHONE", "2145550199"))
        .thenReturn("*******0199");
    when(maskingService.mask("CUSTOMER_CONTACT_T", "CONTACT_EMAIL", "jane@example.com"))
        .thenReturn("@example.com");

    CustomerResponse response = mapper.toResponse(customer);

    assertThat(response.taxId()).isEqualTo("*****6789");
    assertThat(response.contacts().getFirst().phoneNbr()).isEqualTo("*******0199");
    assertThat(response.contacts().getFirst().emailAddr()).isEqualTo("@example.com");
  }

  @Test
  void returnsUnmaskedValuesWhenRequested() {
    CustomerEntity customer = new CustomerEntity();
    customer.setCustId(1);
    customer.setTaxId("123456789");
    customer.setCustName("Jane Doe");
    customer.setCustType("I");
    customer.setCustStatus("A");

    CustomerResponse response = mapper.toResponse(customer, true);

    assertThat(response.taxId()).isEqualTo("123456789");
  }
}
