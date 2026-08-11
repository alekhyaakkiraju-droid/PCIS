package com.pcis.customer.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.customer.config.JpaConfig;
import com.pcis.customer.domain.CustomerAddressEntity;
import com.pcis.customer.domain.CustomerContactEntity;
import com.pcis.customer.domain.CustomerEntity;
import com.pcis.customer.support.PostgresTestContainer;
import com.pcis.customer.support.TestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@EnabledIf("com.pcis.customer.support.TestEnvironment#isDockerAvailable")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    PostgresTestContainer.registerProperties(registry);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private CustomerRepository customerRepository;
  @Autowired private TestEntityManager entityManager;

  @Test
  void findByTaxIdAndCountByTaxId() {
    CustomerEntity customer = new CustomerEntity();
    customer.setTaxId("123456789");
    customer.setCustName("Jane Doe");
    customer.setCustType("I");
    customer.setCustStatus("A");
    entityManager.persist(customer);
    entityManager.flush();

    assertThat(customerRepository.findByTaxId("123456789")).isPresent();
    assertThat(customerRepository.countByTaxId("123456789")).isEqualTo(1);
  }

  @Test
  void findWithDetailsByIdLoadsAddressesAndContacts() {
    CustomerEntity customer = new CustomerEntity();
    customer.setTaxId("987654321");
    customer.setCustName("Acme LLC");
    customer.setCustType("B");
    customer.setCustStatus("A");

    CustomerAddressEntity address = new CustomerAddressEntity();
    address.setAddressLine1("100 Main St");
    address.setCity("Austin");
    address.setStateCode("TX");
    address.setZipCode("78701");
    address.setAddrType("PRM");
    customer.addAddress(address);

    CustomerContactEntity contact = new CustomerContactEntity();
    contact.setFirstName("Bob");
    contact.setLastName("Builder");
    contact.setPhoneNbr("5125550100");
    contact.setEmailAddr("bob@example.com");
    contact.setContactType("PRM");
    customer.addContact(contact);

    entityManager.persist(customer);
    entityManager.flush();
    entityManager.clear();

    CustomerEntity loaded =
        customerRepository.findWithDetailsById(customer.getCustId()).orElseThrow();

    assertThat(loaded.getCustName()).isEqualTo("Acme LLC");
    assertThat(loaded.getAddresses()).hasSize(1);
    assertThat(loaded.getContacts()).hasSize(1);
    assertThat(loaded.getAddresses().iterator().next().getCity()).isEqualTo("Austin");
    assertThat(loaded.getContacts().iterator().next().getLastName()).isEqualTo("Builder");
  }

  @Test
  void uniqueTaxIdConstraintRejectsDuplicate() {
    CustomerEntity first = new CustomerEntity();
    first.setTaxId("555443333");
    first.setCustName("First");
    first.setCustType("I");
    first.setCustStatus("A");
    customerRepository.saveAndFlush(first);

    CustomerEntity duplicate = new CustomerEntity();
    duplicate.setTaxId("555443333");
    duplicate.setCustName("Second");
    duplicate.setCustType("I");
    duplicate.setCustStatus("A");

    assertThatThrownBy(() -> customerRepository.saveAndFlush(duplicate))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }
}
