package com.pcis.customer.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.customer.domain.exception.CustomerNotFoundException;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.domain.model.CreateCustomerCommand;
import com.pcis.customer.domain.model.UpdateCustomerCommand;
import com.pcis.customer.domain.repository.CustomerRepository;
import com.pcis.customer.outbox.OutboxEvent;
import com.pcis.customer.outbox.OutboxEventRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerDomainService {
  private static final String AGGREGATE_TYPE = "Customer";
  private final CustomerRepository customerRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public CustomerDomainService(CustomerRepository customerRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
    this.customerRepository = customerRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CustomerEntity create(CreateCustomerCommand command) {
    assertTaxIdAvailable(command.taxId(), null);
    return persistNewCustomer(command);
  }

  @Transactional
  public CustomerEntity createIgnoringDuplicateCheck(CreateCustomerCommand command) {
    return persistNewCustomer(command);
  }

  private CustomerEntity persistNewCustomer(CreateCustomerCommand command) {
    CustomerEntity customer = new CustomerEntity();
    customer.setTaxId(normalizeTaxId(command.taxId()));
    customer.setCustName(command.custName());
    customer.setCustType(command.custType());
    customer.setCustStatus("A");
    if (command.addresses() != null) command.addresses().forEach(a -> customer.addAddress(toAddressEntity(a)));
    if (command.contacts() != null) command.contacts().forEach(c -> customer.addContact(toContactEntity(c)));
    CustomerEntity saved = customerRepository.save(customer);
    writeOutboxEvent(saved, "CustomerCreated");
    return requireWithDetails(saved.getCustId());
  }

  @Transactional
  public CustomerEntity update(UpdateCustomerCommand command) {
    CustomerEntity customer = customerRepository.findById(command.custId()).orElseThrow(() -> new CustomerNotFoundException(command.custId()));
    assertTaxIdAvailable(command.taxId(), command.custId());
    if (StringUtils.hasText(command.taxId())) customer.setTaxId(normalizeTaxId(command.taxId()));
    if (StringUtils.hasText(command.custName())) customer.setCustName(command.custName());
    if (StringUtils.hasText(command.custType())) customer.setCustType(command.custType());
    if (StringUtils.hasText(command.custStatus())) customer.setCustStatus(command.custStatus());
    CustomerEntity saved = customerRepository.save(customer);
    writeOutboxEvent(saved, "CustomerUpdated");
    return requireWithDetails(saved.getCustId());
  }

  @Transactional(readOnly = true)
  public CustomerEntity findById(Integer custId) { return requireWithDetails(custId); }

  private CustomerEntity requireWithDetails(Integer custId) {
    return customerRepository.findWithDetailsById(custId).orElseThrow(() -> new CustomerNotFoundException(custId));
  }

  private void assertTaxIdAvailable(String taxId, Integer existingCustId) {
    String normalized = normalizeTaxId(taxId);
    if (!StringUtils.hasText(normalized)) return;
    customerRepository.findByTaxId(normalized)
        .filter(existing -> existingCustId == null || !existing.getCustId().equals(existingCustId))
        .ifPresent(existing -> { throw new DuplicateTaxIdException(normalized, toCandidate(existing)); });
  }

  private static DuplicateCandidate toCandidate(CustomerEntity existing) {
    return new DuplicateCandidate(existing.getCustId(), existing.getCustName(), existing.getCustStatus());
  }

  private static String normalizeTaxId(String taxId) { return StringUtils.hasText(taxId) ? taxId.trim() : null; }

  private CustomerAddressEntity toAddressEntity(CreateCustomerCommand.AddressCommand command) {
    CustomerAddressEntity address = new CustomerAddressEntity();
    address.setAddressLine1(command.addressLine1());
    address.setAddressLine2(command.addressLine2());
    address.setCity(command.city());
    address.setStateCode(command.stateCode());
    address.setZipCode(command.zipCode());
    address.setAddrType(StringUtils.hasText(command.addrType()) ? command.addrType() : "PRM");
    return address;
  }

  private CustomerContactEntity toContactEntity(CreateCustomerCommand.ContactCommand command) {
    CustomerContactEntity contact = new CustomerContactEntity();
    contact.setFirstName(command.firstName());
    contact.setLastName(command.lastName());
    contact.setPhoneNbr(command.phoneNbr());
    contact.setEmailAddr(command.emailAddr());
    contact.setContactType(command.contactType());
    return contact;
  }

  private void writeOutboxEvent(CustomerEntity customer, String eventType) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("custId", customer.getCustId());
    payload.put("custName", customer.getCustName());
    payload.put("custType", customer.getCustType());
    payload.put("custStatus", customer.getCustStatus());
    payload.put("taxId", customer.getTaxId());
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType(AGGREGATE_TYPE);
    event.setAggregateId(String.valueOf(customer.getCustId()));
    event.setEventType(eventType);
    event.setPayload(toJson(payload));
    outboxEventRepository.save(event);
  }

  private String toJson(Map<String, Object> payload) {
    try { return objectMapper.writeValueAsString(payload); }
    catch (JsonProcessingException ex) { throw new IllegalStateException("Failed to serialize outbox payload", ex); }
  }
}
