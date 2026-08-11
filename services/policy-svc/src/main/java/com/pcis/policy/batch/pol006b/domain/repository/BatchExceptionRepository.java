package com.pcis.policy.batch.pol006b.domain.repository;

import com.pcis.policy.batch.pol006b.domain.entity.BatchExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExceptionRepository extends JpaRepository<BatchExceptionEntity, Long> {}
