package com.pcis.batch.auth;

import com.pcis.batch.auth.config.BatchAuthProperties;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Obtains and caches OAuth2 client-credentials access tokens for batch workloads.
 */
public class BatchAuthenticationService {

  private final BatchAuthProperties properties;
  private final ClientSecretProvider clientSecretProvider;
  private final RestTemplate restTemplate;
  private final ReentrantLock refreshLock = new ReentrantLock();
  private volatile CachedToken cachedToken;

  public BatchAuthenticationService(
      BatchAuthProperties properties,
      ClientSecretProvider clientSecretProvider,
      RestTemplate restTemplate) {
    this.properties = properties;
    this.clientSecretProvider = clientSecretProvider;
    this.restTemplate = restTemplate;
  }

  /**
   * Returns a valid access token, refreshing proactively before expiry.
   */
  public String getAccessToken() {
    CachedToken current = cachedToken;
    if (isValid(current)) {
      return current.accessToken();
    }

    refreshLock.lock();
    try {
      current = cachedToken;
      if (isValid(current)) {
        return current.accessToken();
      }
      cachedToken = requestToken();
      return cachedToken.accessToken();
    } finally {
      refreshLock.unlock();
    }
  }

  void invalidateCache() {
    cachedToken = null;
  }

  private boolean isValid(CachedToken token) {
    if (token == null) {
      return false;
    }
    Instant refreshAt =
        token.expiresAt().minusSeconds(properties.getExpirationBufferSeconds());
    return Instant.now().isBefore(refreshAt);
  }

  private CachedToken requestToken() {
    validateConfiguration();
    String clientSecret = clientSecretProvider.resolve(properties.getClientSecretRef());

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");
    body.add("client_id", properties.getClientId());
    body.add("client_secret", clientSecret);
    if (StringUtils.hasText(properties.getScope())) {
      body.add("scope", properties.getScope());
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<OAuth2TokenResponse> response =
          restTemplate.postForEntity(
              properties.getTokenUri(), request, OAuth2TokenResponse.class);
      OAuth2TokenResponse tokenResponse = response.getBody();
      if (tokenResponse == null
          || !StringUtils.hasText(tokenResponse.accessToken())
          || tokenResponse.expiresIn() <= 0) {
        throw new BatchConfigurationException("Token endpoint returned an invalid response");
      }
      Instant expiresAt = Instant.now().plusSeconds(tokenResponse.expiresIn());
      return new CachedToken(tokenResponse.accessToken(), expiresAt);
    } catch (RestClientException ex) {
      throw new BatchConfigurationException(
          "Failed to obtain OAuth2 client-credentials token from "
              + properties.getTokenUri(),
          ex);
    }
  }

  private void validateConfiguration() {
    if (!StringUtils.hasText(properties.getTokenUri())) {
      throw new BatchConfigurationException("pcis.batch.oauth2.token-uri must be set");
    }
    if (!StringUtils.hasText(properties.getClientId())) {
      throw new BatchConfigurationException("pcis.batch.oauth2.client-id must be set");
    }
    if (!StringUtils.hasText(properties.getClientSecretRef())) {
      throw new BatchConfigurationException("pcis.batch.oauth2.client-secret-ref must be set");
    }
  }

  record CachedToken(String accessToken, Instant expiresAt) {}
}
