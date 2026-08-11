package com.pcis.observability.propagation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.observability.MdcKeys;
import com.pcis.observability.config.ObservabilityProperties;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

class CorrelationIdRestTemplateCustomizerTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void addsInterceptorAndPropagatesHeaderFromMdc() throws IOException {
    ObservabilityProperties properties = new ObservabilityProperties();
    CorrelationIdRestTemplateCustomizer customizer =
        new CorrelationIdRestTemplateCustomizer(properties);
    RestTemplate restTemplate = new RestTemplate();
    customizer.customize(restTemplate);
    assertThat(restTemplate.getInterceptors()).hasSize(1);

    MDC.put(MdcKeys.CORRELATION_ID, "corr-42");
    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeaders()).thenReturn(headers);
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(execution.execute(any(), any())).thenReturn(response);

    ClientHttpResponse actual =
        restTemplate.getInterceptors().get(0).intercept(request, new byte[0], execution);

    assertThat(actual).isSameAs(response);
    assertThat(headers.getFirst("X-Correlation-ID")).isEqualTo("corr-42");
    verify(execution).execute(eq(request), any());
  }
}
