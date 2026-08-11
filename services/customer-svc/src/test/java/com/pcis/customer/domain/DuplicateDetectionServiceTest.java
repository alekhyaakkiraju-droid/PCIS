package com.pcis.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pcis.customer.domain.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

  @Mock private CustomerRepository customerRepository;
  @InjectMocks private DuplicateDetectionService duplicateDetectionService;

  @Test
  void findsExistingCustomerByTaxId() {
    CustomerEntity existing = new CustomerEntity();
    existing.setCustId(1);
    existing.setTaxId("123456789");
    existing.setCustName("Jane Doe");
    existing.setCustStatus("A");

    when(customerRepository.findByTaxId("123456789")).thenReturn(Optional.of(existing));

    assertThat(duplicateDetectionService.findByTaxId("123456789"))
        .isPresent()
        .get()
        .extracting(DuplicateCandidate::custName)
        .isEqualTo("Jane Doe");
  }

  @Test
  void returnsEmptyWhenTaxIdNotFound() {
    when(customerRepository.findByTaxId("000000000")).thenReturn(Optional.empty());
    assertThat(duplicateDetectionService.findByTaxId("000000000")).isEmpty();
  }
}
