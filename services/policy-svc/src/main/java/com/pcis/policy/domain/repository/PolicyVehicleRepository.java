package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.PolicyVehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyVehicleRepository extends JpaRepository<PolicyVehicleEntity, Long> {}
