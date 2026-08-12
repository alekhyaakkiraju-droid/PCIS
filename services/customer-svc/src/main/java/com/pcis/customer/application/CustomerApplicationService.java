package com.pcis.customer.application;

import com.pcis.customer.domain.CustomerDomainService;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.domain.DuplicateCandidate;
import com.pcis.customer.domain.DuplicateDetectionService;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.domain.model.CreateCustomerCommand;
import com.pcis.customer.domain.model.UpdateCustomerCommand;
import com.pcis.customer.domain.repository.CustomerRepository;
import com.pcis.customer.outbox.CustomerOutboxWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerApplicationService {

  private final DuplicateDetectionService duplicateDetectionService;
  private final CustomerDomainService customerDomainService;
  private final CustomerRepository customerRepository;
  private final CustomerOutboxWriter customerOutboxWriter;

  public CustomerApplicationService(
      DuplicateDetectionService duplicateDetectionService,
      CustomerDomainService customerDomainService,
      CustomerRepository customerRepository,
      CustomerOutboxWriter customerOutboxWriter) {
    this.duplicateDetectionService = duplicateDetectionService;
    this.customerDomainService = customerDomainService;
    this.customerRepository = customerRepository;
    this.customerOutboxWriter = customerOutboxWriter;
  }

  @Transactional
  public CustomerEntity create(CreateCustomerCommand command) {
    Optional<DuplicateCandidate> duplicate = duplicateDetectionService.findByTaxId(command.taxId());
    if (duplicate.isPresent()) {
      DuplicateCandidate existing = duplicate.get();
      writeDuplicateDetectedEvent(command, existing);
      throw new DuplicateTaxIdException(command.taxId(), existing);
    }
    return customerDomainService.create(command);
  }

  @Transactional
  public CustomerEntity createWithOverride(CreateCustomerCommand command, String overrideReason) {
    if (!StringUtils.hasText(overrideReason) || overrideReason.trim().length() < 10) {
      throw new IllegalArgumentException("Override reason must be at least 10 characters");
    }
    DuplicateCandidate duplicate =
        duplicateDetectionService
            .findByTaxId(command.taxId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No duplicate customer exists for the supplied tax ID"));
    CustomerEntity created = customerDomainService.createIgnoringDuplicateCheck(command);
    writeOverrideEvent(created, duplicate, overrideReason.trim());
    return created;
  }

  @Transactional(readOnly = true)
  public CustomerEntity findById(Integer custId) {
    return customerDomainService.findById(custId);
  }

  @Transactional
  public CustomerEntity update(UpdateCustomerCommand command) {
    return customerDomainService.update(command);
  }

  @Transactional(readOnly = true)
  public List<CustomerEntity> search(String query) {
    if (!StringUtils.hasText(query)) {
      throw new IllegalArgumentException("Search query must not be blank");
    }
    return customerRepository.search(query.trim());
  }

  @Transactional(readOnly = true)
  public List<CustomerEntity> list() {
    return customerRepository.findAllWithDetails();
  }

  @Transactional(readOnly = true)
  public Optional<DuplicateCandidate> duplicateCheck(Integer custId) {
    CustomerEntity customer = customerDomainService.findById(custId);
    if (!StringUtils.hasText(customer.getTaxId())) {
      return Optional.empty();
    }
    return duplicateDetectionService.findByTaxId(customer.getTaxId())
        .filter(candidate -> !candidate.custId().equals(custId));
  }

  private void writeDuplicateDetectedEvent(
      CreateCustomerCommand command, DuplicateCandidate duplicate) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("taxId", command.taxId());
    payload.put("requestedCustName", command.custName());
    payload.put("existingCustId", duplicate.custId());
    payload.put("existingCustName", duplicate.custName());
    payload.put("existingCustStatus", duplicate.custStatus());
    writeOutboxEvent(String.valueOf(duplicate.custId()), "DuplicateTaxIdDetected", payload);
  }

  private void writeOverrideEvent(
      CustomerEntity created, DuplicateCandidate duplicate, String overrideReason) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("custId", created.getCustId());
    payload.put("taxId", created.getTaxId());
    payload.put("custName", created.getCustName());
    payload.put("existingCustId", duplicate.custId());
    payload.put("existingCustName", duplicate.custName());
    payload.put("overrideReason", overrideReason);
    writeOutboxEvent(String.valueOf(created.getCustId()), "DuplicateTaxIdOverride", payload);
  }

  private void writeOutboxEvent(String aggregateId, String eventType, Map<String, Object> payload) {
    java.util.UUID idempotencyKey =
        java.util.UUID.nameUUIDFromBytes(
            ("Customer:" + aggregateId + ":" + eventType + ":" + payload.hashCode()).getBytes());
    customerOutboxWriter.writeDomainEvent(aggregateId, eventType, payload, idempotencyKey);
  }
}
