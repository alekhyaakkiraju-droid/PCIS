package com.pcis.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.customer.domain.CustomerDomainService;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.domain.DuplicateCandidate;
import com.pcis.customer.domain.DuplicateDetectionService;
import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.domain.model.CreateCustomerCommand;
import com.pcis.customer.outbox.CustomerOutboxWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

  @Mock private DuplicateDetectionService duplicateDetectionService;
  @Mock private CustomerDomainService customerDomainService;
  @Mock private CustomerOutboxWriter customerOutboxWriter;

  @InjectMocks private CustomerApplicationService customerApplicationService;

  @Test
  void createReturnsCustomerWhenTaxIdIsUnique() {
    CreateCustomerCommand command =
        new CreateCustomerCommand("111223333", "New Customer", "I", List.of(), List.of());
    CustomerEntity created = new CustomerEntity();
    created.setCustId(99);

    when(duplicateDetectionService.findByTaxId("111223333")).thenReturn(Optional.empty());
    when(customerDomainService.create(command)).thenReturn(created);

    assertThat(customerApplicationService.create(command)).isSameAs(created);
    verify(customerOutboxWriter, never()).writeAuditEvent(any(), any(), any());
  }

  @Test
  void createWritesAuditEventAndThrowsWhenDuplicateExists() {
    CreateCustomerCommand command =
        new CreateCustomerCommand("123456789", "Duplicate Attempt", "I", List.of(), List.of());
    DuplicateCandidate duplicate = new DuplicateCandidate(1, "Jane Doe", "A");

    when(duplicateDetectionService.findByTaxId("123456789")).thenReturn(Optional.of(duplicate));

    assertThatThrownBy(() -> customerApplicationService.create(command))
        .isInstanceOf(DuplicateTaxIdException.class)
        .satisfies(
            ex -> {
              DuplicateTaxIdException duplicateEx = (DuplicateTaxIdException) ex;
              assertThat(duplicateEx.getReasonCode()).isEqualTo("DUPLICATE_TAX_ID");
              assertThat(duplicateEx.getExistingCustomer().custId()).isEqualTo(1);
            });

    verify(customerOutboxWriter)
        .writeAuditEvent(eq("1"), eq("DuplicateTaxIdDetected"), any(Map.class));
    verify(customerDomainService, never()).create(any());
  }

  @Test
  void createWithOverridePersistsCustomerAndAuditEvent() {
    CreateCustomerCommand command =
        new CreateCustomerCommand("123456789", "Override Customer", "I", List.of(), List.of());
    DuplicateCandidate duplicate = new DuplicateCandidate(1, "Jane Doe", "A");
    CustomerEntity created = new CustomerEntity();
    created.setCustId(42);
    created.setTaxId("123456789");
    created.setCustName("Override Customer");

    when(duplicateDetectionService.findByTaxId("123456789")).thenReturn(Optional.of(duplicate));
    when(customerDomainService.createIgnoringDuplicateCheck(command)).thenReturn(created);

    CustomerEntity result =
        customerApplicationService.createWithOverride(
            command, "Supervisor approved duplicate for branch merge");

    assertThat(result.getCustId()).isEqualTo(42);
    verify(customerOutboxWriter)
        .writeAuditEvent(eq("42"), eq("DuplicateTaxIdOverride"), any(Map.class));
  }

  @Test
  void createWithOverrideRequiresMinimumReasonLength() {
    CreateCustomerCommand command =
        new CreateCustomerCommand("123456789", "Override Customer", "I", List.of(), List.of());

    assertThatThrownBy(() -> customerApplicationService.createWithOverride(command, "too short"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
