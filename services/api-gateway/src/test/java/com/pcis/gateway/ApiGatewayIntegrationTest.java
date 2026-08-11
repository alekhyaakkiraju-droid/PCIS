package com.pcis.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pcis.gateway.support.JwtTestSupport;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import redis.embedded.RedisServer;

@SpringBootTest(
    classes = ApiGatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayIntegrationTest {

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  private static RedisServer redisServer;
  private static int redisPort;

  static {
    try {
      redisPort = findAvailablePort();
      redisServer = RedisServer.newRedisServer().port(redisPort).build();
      redisServer.start();

      WIRE_MOCK.start();
      WIRE_MOCK.stubFor(
          get(urlEqualTo("/realms/pcis/protocol/openid-connect/certs"))
              .willReturn(
                  aResponse()
                      .withHeader("Content-Type", "application/json")
                      .withBody(JwtTestSupport.jwksJson())));
      WIRE_MOCK.stubFor(
          get(urlEqualTo("/api/v1/customers"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody("{\"status\":\"ok\"}")));
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @Autowired private WebTestClient webTestClient;

  @AfterAll
  static void stopInfrastructure() throws IOException {
    WIRE_MOCK.stop();
    if (redisServer != null) {
      redisServer.stop();
    }
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> JwtTestSupport.ISSUER);
    registry.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> WIRE_MOCK.baseUrl() + "/realms/pcis/protocol/openid-connect/certs");
    registry.add("test.upstream.uri", WIRE_MOCK::baseUrl);
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", () -> redisPort);
  }

  private static int findAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    }
  }

  @Test
  void healthEndpointAccessibleWithoutAuth() {
    webTestClient.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
  }

  @Test
  void unknownPathReturns404WithValidToken() {
    webTestClient
        .get()
        .uri("/api/v1/unknown/resource")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.validToken())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void missingTokenReturns401() {
    webTestClient
        .get()
        .uri("/api/v1/customers")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void validTokenPassesSecurityAndRoutesToUpstream() {
    webTestClient
        .get()
        .uri("/api/v1/customers")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.validToken())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(body -> assertThat(body).contains("ok"));
  }

  @Test
  void expiredTokenReturns401() {
    webTestClient
        .get()
        .uri("/api/v1/customers")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestSupport.expiredToken())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
