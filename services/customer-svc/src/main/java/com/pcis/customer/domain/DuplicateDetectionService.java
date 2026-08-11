package com.pcis.customer.domain;

import com.pcis.customer.domain.repository.CustomerRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DuplicateDetectionService {

  private final CustomerRepository customerRepository;

  public DuplicateDetectionService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public Optional<DuplicateCandidate> findByTaxId(String taxId) {
    String normalized = normalizeTaxId(taxId);
    if (!StringUtils.hasText(normalized)) {
      return Optional.empty();
    }
    return customerRepository.findByTaxId(normalized).map(this::toCandidate);
  }

  private DuplicateCandidate toCandidate(CustomerEntity entity) {
    return new DuplicateCandidate(entity.getCustId(), entity.getCustName(), entity.getCustStatus());
  }

  private static String normalizeTaxId(String taxId) {
    return StringUtils.hasText(taxId) ? taxId.trim() : null;
  }
}
