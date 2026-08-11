package com.pcis.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.customer.domain.exception.CustomerNotFoundException;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.domain.model.CreateCustomerCommand;
import com.pcis.customer.domain.model.UpdateCustomerCommand;
import com.pcis.customer.domain.repository.CustomerRepository;
import com.pcis.customer.outbox.OutboxEvent;
import com.pcis.customer.outbox.OutboxEventRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerDomainServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  private CustomerDomainService customerDomainService;

  @BeforeEach
  void setUp() {
    customerDomainService =
        new CustomerDomainService(
            customerRepository, outboxEventRepository, new ObjectMapper());
  }

  @Test
  void createPersistsCustomerAndOutboxEvent() {
    CustomerEntity persisted = new CustomerEntity();
    persisted.setCustId(42);
    persisted.setTaxId("123456789");
    persisted.setCustName("Jane Doe");
    persisted.setCustType("I");
    persisted.setCustStatus("A");

    when(customerRepository.findByTaxId("123456789")).thenReturn(Optional.empty());
    when(customerRepository.save(any(CustomerEntity.class))).thenReturn(persisted);
    when(customerRepository.findWithDetailsById(42)).thenReturn(Optional.of(persisted));

    CreateCustomerCommand command =
        new CreateCustomerCommand("123456789", "Jane Doe", "I", List.of(), List.of());

    CustomerEntity result = customerDomainService.create(command);

    assertThat(result.getCustId()).isEqualTo(42);
    ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(outboxCaptor.capture());
    assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("CustomerCreated");
    assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo("42");
  }

  @Test
  void createRejectsDuplicateTaxId() {
    CustomerEntity existing = new CustomerEntity();
    existing.setCustId(1);
    existing.setTaxId("123456789");

    when(customerRepository.findByTaxId("123456789")).thenReturn(Optional.of(existing));

    CreateCustomerCommand command =
        new CreateCustomerCommand("123456789", "Jane Doe", "I", List.of(), List.of());

    assertThatThrownBy(() -> customerDomainService.create(command))
        .isInstanceOf(DuplicateTaxIdException.class)
        .hasMessageContaining("123456789");

    verify(customerRepository, never()).save(any());
    verify(outboxEventRepository, never()).save(any());
  }

  @Test
  void updatePersistsChangesAndOutboxEvent() {
    CustomerEntity existing = new CustomerEntity();
    existing.setCustId(7);
    existing.setTaxId("123456789");
    existing.setCustName("Jane Doe");
    existing.setCustType("I");
    existing.setCustStatus("A");

    CustomerEntity updated = new CustomerEntity();
    updated.setCustId(7);
    updated.setTaxId("123456789");
    updated.setCustName("Jane Smith");
    updated.setCustType("I");
    updated.setCustStatus("I");

    when(customerRepository.findById(7)).thenReturn(Optional.of(existing));
    when(customerRepository.findByTaxId("123456789")).thenReturn(Optional.of(existing));
    when(customerRepository.save(existing)).thenReturn(updated);
    when(customerRepository.findWithDetailsById(7)).thenReturn(Optional.of(updated));

    UpdateCustomerCommand command =
        new UpdateCustomerCommand(7, "123456789", "Jane Smith", "I", "I");

    CustomerEntity result = customerDomainService.update(command);

    assertThat(result.getCustName()).isEqualTo("Jane Smith");
    assertThat(result.getCustStatus()).isEqualTo("I");
    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void findByIdThrowsWhenMissing() {
    when(customerRepository.findWithDetailsById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> customerDomainService.findById(99))
        .isInstanceOf(CustomerNotFoundException.class);
  }

  @Test
  void updateThrowsWhenCustomerMissing() {
    when(customerRepository.findById(99)).thenReturn(Optional.empty());

    UpdateCustomerCommand command =
        new UpdateCustomerCommand(99, "123456789", "Jane Doe", "I", "A");

    assertThatThrownBy(() -> customerDomainService.update(command))
        .isInstanceOf(CustomerNotFoundException.class);
  }

  @Test
  void createIncludesNestedAddressAndContact() {
    CustomerEntity persisted = new CustomerEntity();
    persisted.setCustId(10);
    persisted.setCustName("Acme LLC");
    persisted.setCustType("B");
    persisted.setCustStatus("A");

    CustomerAddressEntity address = new CustomerAddressEntity();
    address.setAddressLine1("100 Main St");
    address.setCity("Austin");
    address.setStateCode("TX");
    address.setZipCode("78701");
    persisted.addAddress(address);

    CustomerContactEntity contact = new CustomerContactEntity();
    contact.setFirstName("Bob");
    contact.setLastName("Builder");
    persisted.addContact(contact);

    when(customerRepository.findByTaxId("111223333")).thenReturn(Optional.empty());
    when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
      CustomerEntity entity = invocation.getArgument(0);
      entity.setCustId(10);
      return entity;
    });
    when(customerRepository.findWithDetailsById(10)).thenReturn(Optional.of(persisted));

    CreateCustomerCommand command =
        new CreateCustomerCommand(
            "111223333",
            "Acme LLC",
            "B",
            List.of(
                new CreateCustomerCommand.AddressCommand(
                    "100 Main St", null, "Austin", "TX", "78701", "PRM")),
            List.of(
                new CreateCustomerCommand.ContactCommand(
                    "Bob", "Builder", "5125550100", "bob@example.com", "PRM")));

    CustomerEntity result = customerDomainService.create(command);

    assertThat(result.getAddresses()).hasSize(1);
    assertThat(result.getContacts()).hasSize(1);
    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }
}
