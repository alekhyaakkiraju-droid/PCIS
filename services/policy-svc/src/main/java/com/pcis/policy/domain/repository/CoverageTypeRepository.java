package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.CoverageTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageTypeRepository extends JpaRepository<CoverageTypeEntity, String> {}
