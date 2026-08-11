package com.pcis.batch.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pcis.batch.auth.config.BatchAuthProperties;
import com.pcis.batch.auth.support.TestJwtFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Integration test against a WireMock OAuth2 token endpoint (no Docker / Keycloak required).
 */
class BatchAuthWireMockIntegrationTest {

  private static WireMockServer wireMock;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMock != null) {
      wireMock.stop();
    }
  }

  @Test
  void obtainsTokenAndPropagatesBearerHeaderOnOutboundCall() {
    String jwt = TestJwtFactory.tokenWithSubject("service-account-batch-premium");
    wireMock.stubFor(
        post(urlEqualTo("/token"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"access_token\":\""
                            + jwt
                            + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

    wireMock.stubFor(
        post(urlEqualTo("/downstream"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "text/plain")
                    .withBody("ok")));

    BatchAuthProperties properties = new BatchAuthProperties();
    properties.setTokenUri(wireMock.baseUrl() + "/token");
    properties.setClientId("batch-premium");
    properties.setClientSecretRef(
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:batch-premium");
    properties.setScope("batch:premium");

    RestTemplate tokenRestTemplate = new RestTemplate();
    BatchAuthenticationService authService =
        new BatchAuthenticationService(
            properties, secretRef -> "integration-secret", tokenRestTemplate);

    RestTemplate outbound = new RestTemplate();
    outbound.getInterceptors().add(new BatchAuthRestTemplateInterceptor(authService));

    ResponseEntity<String> response =
        outbound.exchange(
            wireMock.baseUrl() + "/downstream", HttpMethod.POST, HttpEntity.EMPTY, String.class);

    assertThat(response.getBody()).isEqualTo("ok");
    wireMock.verify(1, postRequestedFor(urlEqualTo("/token")).withRequestBody(containing("batch-premium")));
    wireMock.verify(
        1,
        postRequestedFor(urlEqualTo("/downstream"))
            .withHeader(HttpHeaders.AUTHORIZATION, containing("Bearer ")));
  }
}
