package com.pcis.policy.batch.pol006b.client;

import com.pcis.policy.batch.pol006b.exception.PremiumServiceUnavailableException;
import com.pcis.policy.config.PremiumProperties;
import com.pcis.policy.domain.entity.PolicyEntity;
import com.pcis.policy.domain.entity.PolicyPropertyEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class PremiumServiceClient {

  private final RestClient restClient;

  public PremiumServiceClient(PremiumProperties premiumProperties) {
    this.restClient = RestClient.builder().baseUrl(premiumProperties.getSvcUrl()).build();
  }

  @CircuitBreaker(name = "premium-svc", fallbackMethod = "rateFallback")
  public RatingResponse rate(PolicyEntity policy, String stateCode) {
    RatingRequest request =
        new RatingRequest(
            trimPolicyType(policy.getPolicyType()),
            stateCode,
            policy.getPremAnnual().toPlainString(),
            policy.getPolNbr());
    try {
      RatingResponsePayload body =
          restClient
              .post()
              .uri("/api/v1/premium/calculations")
              .body(request)
              .retrieve()
              .body(RatingResponsePayload.class);
      if (body == null) {
        throw new PremiumServiceUnavailableException(
            policy.getPolNbr(), new IllegalStateException("empty premium-svc response"));
      }
      return new RatingResponse(
          body.calculationId(),
          body.returnCode(),
          body.underwritingDecision(),
          body.finalPremium());
    } catch (RestClientException ex) {
      throw new PremiumServiceUnavailableException(policy.getPolNbr(), ex);
    }
  }

  @SuppressWarnings("unused")
  private RatingResponse rateFallback(PolicyEntity policy, String stateCode, Throwable cause) {
    throw new PremiumServiceUnavailableException(policy.getPolNbr(), cause);
  }

  public static String resolveStateCode(PolicyEntity policy) {
    return policy.getProperties().stream()
        .findFirst()
        .map(PolicyPropertyEntity::getStateCode)
        .map(String::trim)
        .filter(code -> !code.isEmpty())
        .orElse("TX");
  }

  private static String trimPolicyType(String policyType) {
    return policyType == null ? "" : policyType.trim();
  }
}
