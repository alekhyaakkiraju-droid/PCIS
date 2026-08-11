package com.pcis.batch.auth.support;

import com.pcis.batch.auth.BatchAuthenticationService;
import com.pcis.batch.auth.config.BatchAuthProperties;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.client.RestTemplate;

/**
 * Test double for {@link BatchAuthenticationService} used by downstream batch module tests.
 */
public class MockBatchAuthenticationService extends BatchAuthenticationService {

  private final AtomicInteger tokenRequests = new AtomicInteger();
  private volatile String accessToken;
  private volatile Instant expiresAt = Instant.now().plusSeconds(3600);

  public MockBatchAuthenticationService() {
    super(new BatchAuthProperties(), secretRef -> "mock-secret", new RestTemplate());
  }

  public MockBatchAuthenticationService withAccessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  public MockBatchAuthenticationService expiringAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  @Override
  public String getAccessToken() {
    tokenRequests.incrementAndGet();
    if (accessToken == null) {
      accessToken = TestJwtFactory.tokenWithSubject("service-account-batch-test");
    }
    return accessToken;
  }

  public int getTokenRequestCount() {
    return tokenRequests.get();
  }
}
