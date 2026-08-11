package com.pcis.batch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.batch.auth.config.BatchAuthProperties;
import com.pcis.batch.auth.support.TestJwtFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BatchAuthenticationServiceTest {

  private BatchAuthProperties properties;
  private RestTemplate restTemplate;
  private MockRestServiceServer server;
  private BatchAuthenticationService service;

  @BeforeEach
  void setUp() {
    properties = new BatchAuthProperties();
    properties.setTokenUri("http://idp.example.com/oauth/token");
    properties.setClientId("batch-billing");
    properties.setClientSecretRef("arn:aws:secretsmanager:us-east-1:123456789012:secret:batch-billing");
    properties.setScope("batch:billing");
    properties.setExpirationBufferSeconds(30);

    restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    service =
        new BatchAuthenticationService(
            properties, secretRef -> "test-client-secret", restTemplate);
  }

  @Test
  void cachesTokenWithinLifetime() {
    stubTokenResponse("token-a", 3600);

    assertThat(service.getAccessToken()).isEqualTo("token-a");
    assertThat(service.getAccessToken()).isEqualTo("token-a");

    server.verify();
  }

  @Test
  void refreshesTokenWhenInsideExpirationBuffer() {
    stubTokenResponse("token-a", 20);
    assertThat(service.getAccessToken()).isEqualTo("token-a");

    server.reset();
    stubTokenResponse("token-b", 3600);
    assertThat(service.getAccessToken()).isEqualTo("token-b");
  }

  @Test
  void sendsClientCredentialsRequest() {
    server
        .expect(requestTo(properties.getTokenUri()))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=batch-billing")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=test-client-secret")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("scope=batch%3Abilling")))
        .andRespond(
            withSuccess(
                "{\"access_token\":\"abc\",\"token_type\":\"Bearer\",\"expires_in\":3600}",
                MediaType.APPLICATION_JSON));

    assertThat(service.getAccessToken()).isEqualTo("abc");
  }

  @Test
  void throwsBatchConfigurationExceptionWhenTokenEndpointFails() {
    server
        .expect(requestTo(properties.getTokenUri()))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(service::getAccessToken)
        .isInstanceOf(BatchConfigurationException.class)
        .hasMessageContaining("Failed to obtain OAuth2")
        .satisfies(
            ex -> assertThat(((BatchConfigurationException) ex).getExitCode()).isEqualTo(5));
  }

  @Test
  void throwsWhenConfigurationIncomplete() {
    properties.setClientId("");
    assertThatThrownBy(service::getAccessToken).isInstanceOf(BatchConfigurationException.class);
  }

  @Test
  void invalidateCacheForcesRefresh() {
    stubTokenResponse("token-a", 3600);
    assertThat(service.getAccessToken()).isEqualTo("token-a");

    service.invalidateCache();
    server.reset();
    stubTokenResponse("token-b", 3600);
    assertThat(service.getAccessToken()).isEqualTo("token-b");
  }

  private void stubTokenResponse(String accessToken, long expiresInSeconds) {
    server
        .expect(requestTo(properties.getTokenUri()))
        .andRespond(
            withSuccess(
                "{\"access_token\":\""
                    + accessToken
                    + "\",\"token_type\":\"Bearer\",\"expires_in\":"
                    + expiresInSeconds
                    + "}",
                MediaType.APPLICATION_JSON));
  }
}
