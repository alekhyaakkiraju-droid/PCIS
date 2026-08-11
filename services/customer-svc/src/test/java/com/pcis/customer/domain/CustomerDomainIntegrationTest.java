package com.pcis.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.customer.domain.exception.DuplicateTaxIdException;
import com.pcis.customer.domain.model.CreateCustomerCommand;
import com.pcis.customer.domain.model.UpdateCustomerCommand;
import com.pcis.customer.domain.repository.CustomerRepository;
import com.pcis.customer.outbox.OutboxEventRepository;
import com.pcis.customer.support.PostgresTestContainer;
import com.pcis.customer.support.TestEnvironment;
import com.pcis.customer.support.TestSecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnabledIf("com.pcis.customer.support.TestEnvironment#isDockerAvailable")
class CustomerDomainIntegrationTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
  }

  @Autowired private CustomerDomainService customerDomainService;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private javax.sql.DataSource dataSource;

  @Test
  @Transactional
  void createUpdateAndFindCustomerWithOutboxEvents() {
    CreateCustomerCommand createCommand =
        new CreateCustomerCommand(
            "222334444",
            "Integration Customer",
            "I",
            List.of(
                new CreateCustomerCommand.AddressCommand(
                    "200 Oak Ave", "Suite 5", "Dallas", "TX", "75201", "PRM")),
            List.of(
                new CreateCustomerCommand.ContactCommand(
                    "Pat", "Lee", "2145550199", "pat.lee@example.com", "PRM")));

    CustomerEntity created = customerDomainService.create(createCommand);

    assertThat(created.getCustId()).isNotNull();
    assertThat(created.getAddresses()).hasSize(1);
    assertThat(created.getContacts()).hasSize(1);
    assertThat(outboxEventRepository.findAll())
        .anyMatch(event -> "CustomerCreated".equals(event.getEventType()));

    UpdateCustomerCommand updateCommand =
        new UpdateCustomerCommand(
            created.getCustId(), "222334444", "Updated Customer", "I", "I");
    CustomerEntity updated = customerDomainService.update(updateCommand);

    assertThat(updated.getCustName()).isEqualTo("Updated Customer");
    assertThat(updated.getCustStatus()).isEqualTo("I");
    assertThat(outboxEventRepository.findAll())
        .anyMatch(event -> "CustomerUpdated".equals(event.getEventType()));

    CustomerEntity found = customerDomainService.findById(created.getCustId());
    assertThat(found.getCustName()).isEqualTo("Updated Customer");
  }

  @Test
  @Transactional
  void duplicateTaxIdIsRejected() {
    CreateCustomerCommand first =
        new CreateCustomerCommand("333445555", "First Customer", "I", List.of(), List.of());
    customerDomainService.create(first);

    CreateCustomerCommand duplicate =
        new CreateCustomerCommand("333445555", "Second Customer", "I", List.of(), List.of());

    assertThatThrownBy(() -> customerDomainService.create(duplicate))
        .isInstanceOf(DuplicateTaxIdException.class);
  }

  @Test
  void fixtureSeedDataLoads() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource("test-data/customer-fixtures.sql"));
    populator.execute(dataSource);

    assertThat(customerRepository.findByTaxId("123456789")).isPresent();
    assertThat(customerRepository.findByTaxId("987654321")).isPresent();
    assertThat(customerRepository.count()).isGreaterThanOrEqualTo(4);
  }
}
