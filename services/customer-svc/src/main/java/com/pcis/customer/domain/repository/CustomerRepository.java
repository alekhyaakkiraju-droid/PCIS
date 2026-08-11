package com.pcis.customer.domain.repository;

import com.pcis.customer.domain.CustomerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Integer> {
  Optional<CustomerEntity> findByTaxId(String taxId);
  long countByTaxId(String taxId);
  @Query("SELECT DISTINCT c FROM CustomerEntity c LEFT JOIN FETCH c.addresses LEFT JOIN FETCH c.contacts WHERE c.custId = :custId")
  Optional<CustomerEntity> findWithDetailsById(@Param("custId") Integer custId);
}
