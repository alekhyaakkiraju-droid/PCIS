package com.pcis.policy.service;

import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.repository.PolicyRepository;
import com.pcis.policy.dto.CustomerPolicySummaryResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerPolicySummaryService {

  private static final String ACTIVE_STATUS = "ACTV";

  private final PolicyRepository policyRepository;

  public CustomerPolicySummaryService(PolicyRepository policyRepository) {
    this.policyRepository = policyRepository;
  }

  @Transactional(readOnly = true)
  public CustomerPolicySummaryResponse getSummary(Integer custId) {
    List<PolicyEntity> activePolicies =
        policyRepository.findByFilters(custId, ACTIVE_STATUS, Pageable.unpaged()).getContent();

    List<CustomerPolicySummaryResponse.PolicyItem> items =
        activePolicies.stream()
            .map(
                policy ->
                    new CustomerPolicySummaryResponse.PolicyItem(
                        policy.getPolNbr(),
                        policy.getPolicyType().trim(),
                        mapStatus(policy.getPolStatus()),
                        policy.getPremAnnual()))
            .toList();

    return new CustomerPolicySummaryResponse(items.size(), items);
  }

  private static String mapStatus(String polStatus) {
    if (polStatus == null) {
      return "U";
    }
    String trimmed = polStatus.trim();
    return "ACTV".equalsIgnoreCase(trimmed) ? "A" : trimmed;
  }
}
