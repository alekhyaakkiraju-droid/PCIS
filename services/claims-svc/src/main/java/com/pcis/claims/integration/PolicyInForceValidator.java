package com.pcis.claims.integration;

import com.pcis.claims.exception.PolicyNotInForceException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PolicyInForceValidator {

  private static final Logger log = LoggerFactory.getLogger(PolicyInForceValidator.class);

  private final RestClient policyRestClient;
  private final boolean enabled;

  public PolicyInForceValidator(
      RestClient policyRestClient, @Value("${pcis.policy-validation.enabled:true}") boolean enabled) {
    this.policyRestClient = policyRestClient;
    this.enabled = enabled;
  }

  public PolicySnapshot validate(String polNbr, Integer custId, LocalDate lossDate) {
    if (!enabled) {
      return null;
    }
    try {
      PolicySnapshot policy =
          policyRestClient
              .get()
              .uri("/api/v1/policies/{policyNumber}", polNbr)
              .retrieve()
              .body(PolicySnapshot.class);
      if (policy == null) {
        throw new PolicyNotInForceException(polNbr, "policy not found");
      }
      if (custId != null && policy.customerId() != null && !custId.equals(policy.customerId())) {
        throw new PolicyNotInForceException(polNbr, "customer does not match policy");
      }
      if (policy.status() != null && !isInForceStatus(policy.status())) {
        throw new PolicyNotInForceException(polNbr, "policy status is " + policy.status());
      }
      if (policy.effectiveDate() != null && lossDate.isBefore(policy.effectiveDate())) {
        throw new PolicyNotInForceException(polNbr, "loss date before effective date");
      }
      if (policy.expirationDate() != null && lossDate.isAfter(policy.expirationDate())) {
        throw new PolicyNotInForceException(polNbr, "loss date after expiration date");
      }
      return policy;
    } catch (RestClientException ex) {
      log.warn("Policy service unavailable for {} — skipping in-force validation", polNbr);
      return null;
    }
  }

  public record PolicySnapshot(
      String policyNumber,
      Integer customerId,
      String policyType,
      String status,
      LocalDate effectiveDate,
      LocalDate expirationDate) {}

  private static boolean isInForceStatus(String status) {
    String normalized = status.trim().toUpperCase();
    return "ACTV".equals(normalized) || "ACTIVE".equals(normalized);
  }
}
