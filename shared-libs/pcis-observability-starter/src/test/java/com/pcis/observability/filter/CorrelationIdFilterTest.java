package com.pcis.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pcis.observability.MdcKeys;
import com.pcis.observability.config.ObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private CorrelationIdFilter filter;
  private ObservabilityProperties properties;

  @BeforeEach
  void setUp() {
    properties = new ObservabilityProperties();
    filter = new CorrelationIdFilter(properties, "policy-svc");
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void usesInboundCorrelationHeaderWhenValid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/policies/1");
    String inbound = "abc-123_CORRELATION.id";
    request.addHeader(CorrelationIdFilter.CORRELATION_HEADER, inbound);
    request.addHeader(CorrelationIdFilter.HEADER_PROGRAM, "POL001A");
    request.addHeader(CorrelationIdFilter.HEADER_ACTOR, "underwriter1");
    request.addHeader(CorrelationIdFilter.HEADER_RESOURCE, "policy");
    request.addHeader(CorrelationIdFilter.HEADER_OPERATION, "read");

    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInChain = new AtomicReference<>();

    FilterChain chain = mock(FilterChain.class);
    doAnswer(
            invocation -> {
              seenInChain.set(MDC.get(MdcKeys.CORRELATION_ID));
              assertThat(MDC.get(MdcKeys.SERVICE)).isEqualTo("policy-svc");
              assertThat(MDC.get(MdcKeys.PROGRAM)).isEqualTo("POL001A");
              assertThat(MDC.get(MdcKeys.ACTOR)).isEqualTo("underwriter1");
              assertThat(MDC.get(MdcKeys.RESOURCE)).isEqualTo("policy");
              assertThat(MDC.get(MdcKeys.OPERATION)).isEqualTo("read");
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    filter.doFilter(request, response, chain);

    assertThat(seenInChain.get()).isEqualTo(inbound);
    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_HEADER)).isEqualTo(inbound);
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    verify(chain).doFilter(any(), any());
  }

  @Test
  void generatesUuidWhenHeaderAbsent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/claims");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seen = new AtomicReference<>();

    FilterChain chain = mock(FilterChain.class);
    doAnswer(
            invocation -> {
              seen.set(MDC.get(MdcKeys.CORRELATION_ID));
              assertThat(MDC.get(MdcKeys.RESOURCE)).isEqualTo("/claims");
              assertThat(MDC.get(MdcKeys.OPERATION)).isEqualTo("POST");
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    filter.doFilter(request, response, chain);

    assertThat(seen.get()).isNotBlank();
    assertThat(UUID.fromString(seen.get())).isNotNull();
    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_HEADER)).isEqualTo(seen.get());
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  void sanitizesInvalidHeaderWithControlCharacters() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.addHeader(CorrelationIdFilter.CORRELATION_HEADER, "evil\r\nInjected: true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seen = new AtomicReference<>();

    FilterChain chain = mock(FilterChain.class);
    doAnswer(
            invocation -> {
              seen.set(MDC.get(MdcKeys.CORRELATION_ID));
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    filter.doFilter(request, response, chain);

    assertThat(seen.get()).doesNotContain("\r").doesNotContain("\n").doesNotContain("Injected");
    assertThat(UUID.fromString(seen.get())).isNotNull();
    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_HEADER)).isEqualTo(seen.get());
  }

  @Test
  void clearsMdcEvenWhenFilterChainThrows() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    doThrow(new ServletException("boom")).when(chain).doFilter(any(), any());

    try {
      filter.doFilter(request, response, chain);
    } catch (ServletException expected) {
      assertThat(expected.getMessage()).isEqualTo("boom");
    }

    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  void clearsMdcWhenChainThrowsIoException() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/io");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    doThrow(new IOException("io")).when(chain).doFilter(any(), any());

    try {
      filter.doFilter(request, response, chain);
    } catch (IOException expected) {
      assertThat(expected.getMessage()).isEqualTo("io");
    }

    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  void resolveCorrelationIdRejectsSpacesAndEmpty() {
    assertThat(filter.resolveCorrelationId(null)).satisfies(id -> UUID.fromString(id));
    assertThat(filter.resolveCorrelationId("   ")).satisfies(id -> UUID.fromString(id));
    assertThat(filter.resolveCorrelationId("bad value")).satisfies(id -> UUID.fromString(id));
    assertThat(CorrelationIdFilter.isSafeCorrelationId("ok-id_1")).isTrue();
    assertThat(CorrelationIdFilter.isSafeCorrelationId("has space")).isFalse();
  }

  @Test
  void usesPrincipalNameAsActorWhenHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/me");
    request.setUserPrincipal(() -> "batch-runner");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> actor = new AtomicReference<>();

    FilterChain chain = mock(FilterChain.class);
    doAnswer(
            invocation -> {
              actor.set(MDC.get(MdcKeys.ACTOR));
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    filter.doFilter(request, response, chain);
    assertThat(actor.get()).isEqualTo("batch-runner");
  }
}
