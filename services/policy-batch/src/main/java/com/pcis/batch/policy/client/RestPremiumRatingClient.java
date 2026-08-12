package com.pcis.batch.policy.client;

import com.pcis.batch.policy.config.PolicyRenewalProperties;
import com.pcis.batch.policy.domain.RatingUnavailableException;
import com.pcis.batch.policy.domain.RenewalCandidateRow;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestPremiumRatingClient implements PremiumRatingClient {

  private final RestTemplate restTemplate;
  private final PolicyRenewalProperties properties;

  public RestPremiumRatingClient(
      @Qualifier("batchAuthRestTemplate") RestTemplate restTemplate,
      PolicyRenewalProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @Override
  public PremiumRatingResponse rateRenewal(RenewalCandidateRow candidate) {
    PremiumRatingRequest request =
        new PremiumRatingRequest(
            candidate.policyType(),
            candidate.stateCode(),
            candidate.premAnnual().toPlainString(),
            candidate.polNbr());
    try {
      Map<?, ?> body =
          restTemplate.postForObject(
              properties.getPremiumSvcUrl() + "/api/v1/premium/calculations",
              request,
              Map.class);
      if (body == null) {
        throw new RatingUnavailableException(candidate.polNbr(), new IllegalStateException("empty body"));
      }
      return new PremiumRatingResponse(
          stringValue(body.get("calculationId")),
          stringValue(body.get("returnCode")),
          stringValue(body.get("underwritingDecision")),
          new BigDecimal(stringValue(body.get("finalPremium"))));
    } catch (RestClientException ex) {
      throw new RatingUnavailableException(candidate.polNbr(), ex);
    }
  }

  private static String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }
}
