package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.DeductibleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeductibleRepository extends JpaRepository<DeductibleEntity, Long> {}
