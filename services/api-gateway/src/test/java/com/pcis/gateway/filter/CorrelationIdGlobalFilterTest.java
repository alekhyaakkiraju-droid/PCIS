package com.pcis.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CorrelationIdGlobalFilterTest {

  private CorrelationIdGlobalFilter filter;
  private GatewayFilterChain chain;

  @BeforeEach
  void setUp() {
    filter = new CorrelationIdGlobalFilter();
    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  void generatesCorrelationIdWhenHeaderMissing() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/customers").build());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    String correlationId =
        exchange.getAttribute(CorrelationIdGlobalFilter.CORRELATION_ATTRIBUTE);
    assertThat(correlationId).isNotBlank();
    assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_HEADER))
        .isEqualTo(correlationId);
  }

  @Test
  void preservesValidIncomingCorrelationId() {
    String incoming = "abc123-test-correlation";
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/customers")
                .header(CorrelationIdGlobalFilter.CORRELATION_HEADER, incoming)
                .build());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    String correlationId =
        (String) exchange.getAttribute(CorrelationIdGlobalFilter.CORRELATION_ATTRIBUTE);
    assertThat(correlationId).isEqualTo(incoming);
    assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_HEADER))
        .isEqualTo(incoming);
  }

  @Test
  void rejectsUnsafeCorrelationIdAndGeneratesNewOne() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/customers")
                .header(CorrelationIdGlobalFilter.CORRELATION_HEADER, "bad\ninjection")
                .build());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    String correlationId =
        exchange.getAttribute(CorrelationIdGlobalFilter.CORRELATION_ATTRIBUTE);
    assertThat(correlationId).isNotNull().isNotEqualTo("bad\ninjection");
    assertThat(CorrelationIdGlobalFilter.isSafeCorrelationId("bad\ninjection")).isFalse();
  }

  @Test
  void propagatesCorrelationIdToDownstreamRequest() {
    String incoming = "downstream-correlation-id";
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/customers")
                .header(CorrelationIdGlobalFilter.CORRELATION_HEADER, incoming)
                .build());

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(chain).filter(captor.capture());
    HttpHeaders downstreamHeaders = captor.getValue().getRequest().getHeaders();
    assertThat(downstreamHeaders.getFirst(CorrelationIdGlobalFilter.CORRELATION_HEADER))
        .isEqualTo(incoming);
  }

  @Test
  void resolveCorrelationIdReturnsUuidForBlankHeader() {
    String generatedFromNull = filter.resolveCorrelationId(null);
    String generatedFromBlank = filter.resolveCorrelationId("   ");
    assertThat(generatedFromNull).matches("[0-9a-f\\-]{36}");
    assertThat(generatedFromBlank).matches("[0-9a-f\\-]{36}");
  }
}
