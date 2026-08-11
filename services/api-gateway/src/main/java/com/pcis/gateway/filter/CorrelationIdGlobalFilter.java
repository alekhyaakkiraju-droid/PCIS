package com.pcis.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Generates or preserves {@code X-Correlation-ID}, stores it in exchange attributes and MDC,
 * and propagates it to downstream requests.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

  public static final String CORRELATION_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ATTRIBUTE = "correlationId";

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);

  private static final Pattern SAFE_CORRELATION_ID =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String correlationId = resolveCorrelationId(exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER));
    exchange.getAttributes().put(CORRELATION_ATTRIBUTE, correlationId);

    ServerHttpRequest mutatedRequest =
        exchange
            .getRequest()
            .mutate()
            .header(CORRELATION_HEADER, correlationId)
            .build();

    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
    mutatedExchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);

    MDC.put(CORRELATION_ATTRIBUTE, correlationId);
    return chain
        .filter(mutatedExchange)
        .doFinally(signalType -> MDC.remove(CORRELATION_ATTRIBUTE));
  }

  String resolveCorrelationId(String rawHeader) {
    if (!StringUtils.hasText(rawHeader)) {
      return UUID.randomUUID().toString();
    }
    String candidate = rawHeader.trim();
    if (!isSafeCorrelationId(candidate)) {
      log.debug("Rejecting invalid X-Correlation-ID header value");
      return UUID.randomUUID().toString();
    }
    return candidate;
  }

  static boolean isSafeCorrelationId(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\0') >= 0) {
      return false;
    }
    return SAFE_CORRELATION_ID.matcher(value).matches();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
