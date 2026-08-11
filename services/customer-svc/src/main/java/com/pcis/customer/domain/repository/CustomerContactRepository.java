package com.pcis.customer.domain.repository;

import com.pcis.customer.domain.CustomerContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContactEntity, Long> {}
