package com.pcis.observability.propagation;

import com.pcis.observability.MdcKeys;
import com.pcis.observability.config.ObservabilityProperties;
import com.pcis.observability.filter.CorrelationIdFilter;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Propagates the current MDC correlation id on outbound {@link RestTemplate} calls.
 */
public class CorrelationIdRestTemplateCustomizer implements RestTemplateCustomizer {

  private final ObservabilityProperties properties;

  public CorrelationIdRestTemplateCustomizer(ObservabilityProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(RestTemplate restTemplate) {
    restTemplate.getInterceptors().add(new CorrelationIdInterceptor(properties));
  }

  static final class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private final ObservabilityProperties properties;

    CorrelationIdInterceptor(ObservabilityProperties properties) {
      this.properties = properties;
    }

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
      if (StringUtils.hasText(correlationId)) {
        String header =
            StringUtils.hasText(properties.getCorrelationHeader())
                ? properties.getCorrelationHeader()
                : CorrelationIdFilter.CORRELATION_HEADER;
        request.getHeaders().set(header, correlationId);
      }
      return execution.execute(request, body);
    }
  }
}
