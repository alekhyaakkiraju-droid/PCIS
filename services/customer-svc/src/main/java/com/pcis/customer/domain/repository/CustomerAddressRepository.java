package com.pcis.customer.domain.repository;

import com.pcis.customer.domain.CustomerAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, Long> {}
