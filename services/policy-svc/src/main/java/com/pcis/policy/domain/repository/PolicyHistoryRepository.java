package com.pcis.policy.domain.repository;

import com.pcis.policy.domain.entity.PolicyHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyHistoryRepository extends JpaRepository<PolicyHistoryEntity, Long> {}
