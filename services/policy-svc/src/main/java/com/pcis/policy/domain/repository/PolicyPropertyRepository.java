package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.PolicyPropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyPropertyRepository extends JpaRepository<PolicyPropertyEntity, Long> {}
