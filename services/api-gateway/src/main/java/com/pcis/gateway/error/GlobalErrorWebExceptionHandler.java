package com.pcis.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.gateway.filter.CorrelationIdGlobalFilter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Normalises gateway errors as RFC 9457 JSON problem details. */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

  private static final URI DEFAULT_TYPE = URI.create("about:blank");

  private final ObjectMapper objectMapper;

  public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(ex);
    }

    HttpStatus status = resolveStatus(ex);
    String correlationId = resolveCorrelationId(exchange);
    ProblemDetail problem =
        ProblemDetail.builder()
            .type(DEFAULT_TYPE)
            .title(status.getReasonPhrase())
            .status(status.value())
            .detail(resolveDetail(ex, status))
            .instance(exchange.getRequest().getURI())
            .correlationId(correlationId)
            .build();

    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    if (correlationId != null) {
      exchange.getResponse().getHeaders().set(CorrelationIdGlobalFilter.CORRELATION_HEADER, correlationId);
    }

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(problem);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (Exception serializationError) {
      return Mono.error(serializationError);
    }
  }

  HttpStatus resolveStatus(Throwable ex) {
    if (ex instanceof ResponseStatusException responseStatusException) {
      return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
    }
    if (ex instanceof AuthenticationException) {
      return HttpStatus.UNAUTHORIZED;
    }
    if (ex instanceof AccessDeniedException) {
      return HttpStatus.FORBIDDEN;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  String resolveDetail(Throwable ex, HttpStatus status) {
    if (status == HttpStatus.UNAUTHORIZED) {
      return "Authentication is required to access this resource";
    }
    if (status == HttpStatus.FORBIDDEN) {
      return "You do not have permission to access this resource";
    }
    if (ex instanceof ResponseStatusException responseStatusException
        && responseStatusException.getReason() != null) {
      return responseStatusException.getReason();
    }
    return status.getReasonPhrase();
  }

  String resolveCorrelationId(ServerWebExchange exchange) {
    Object attribute = exchange.getAttribute(CorrelationIdGlobalFilter.CORRELATION_ATTRIBUTE);
    if (attribute instanceof String correlationId) {
      return correlationId;
    }
    String header = exchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_HEADER);
    return header;
  }
}
