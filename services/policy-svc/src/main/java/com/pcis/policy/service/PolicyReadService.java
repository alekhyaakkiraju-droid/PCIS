package com.pcis.policy.service;

import com.pcis.error.ResourceNotFoundException;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.repository.PolicyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyReadService {

  private final PolicyRepository policyRepository;

  public PolicyReadService(PolicyRepository policyRepository) {
    this.policyRepository = policyRepository;
  }

  @Transactional(readOnly = true)
  public PolicyEntity findByPolNbr(String polNbr) {
    return policyRepository
        .findWithDetailsByPolNbr(polNbr)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Policy not found: " + polNbr, "system", "policy:" + polNbr, "read"));
  }

  @Transactional(readOnly = true)
  public List<PolicyEntity> findAll() {
    return policyRepository.findAll();
  }
}
