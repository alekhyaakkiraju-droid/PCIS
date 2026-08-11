package com.pcis.observability.filter;

import com.pcis.observability.MdcKeys;
import com.pcis.observability.config.ObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts or generates {@code X-Correlation-ID}, populates MDC context keys, echoes the header on
 * the response, and always clears MDC in {@code finally}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

  /** Accept UUID or a conservative opaque token; reject CR/LF and other injection vectors. */
  private static final Pattern SAFE_CORRELATION_ID =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");

  private static final Pattern SAFE_CONTEXT_TOKEN =
      Pattern.compile("^[A-Za-z0-9/][A-Za-z0-9._:@/-]{0,127}$");

  public static final String CORRELATION_HEADER = "X-Correlation-ID";
  public static final String HEADER_PROGRAM = "X-Program";
  public static final String HEADER_ACTOR = "X-Actor";
  public static final String HEADER_RESOURCE = "X-Resource";
  public static final String HEADER_OPERATION = "X-Operation";

  private final ObservabilityProperties properties;
  private final String serviceName;

  public CorrelationIdFilter(ObservabilityProperties properties, String serviceName) {
    this.properties = properties;
    this.serviceName = StringUtils.hasText(serviceName) ? serviceName : "pcis-service";
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String headerName =
        StringUtils.hasText(properties.getCorrelationHeader())
            ? properties.getCorrelationHeader()
            : CORRELATION_HEADER;

    String correlationId;
    try {
      correlationId = resolveCorrelationId(request.getHeader(headerName));
      populateMdc(request, correlationId);
      response.setHeader(headerName, correlationId);
    } catch (RuntimeException ex) {
      log.warn("Failed to initialize correlation MDC; generating fallback id", ex);
      correlationId = UUID.randomUUID().toString();
      MDC.put(MdcKeys.CORRELATION_ID, correlationId);
      MDC.put(MdcKeys.SERVICE, serviceName);
      response.setHeader(headerName, correlationId);
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private void populateMdc(HttpServletRequest request, String correlationId) {
    MDC.put(MdcKeys.CORRELATION_ID, correlationId);
    MDC.put(MdcKeys.SERVICE, serviceName);
    putIfSafe(MdcKeys.PROGRAM, request.getHeader(HEADER_PROGRAM));
    putIfSafe(MdcKeys.ACTOR, resolveActor(request));
    putIfSafe(MdcKeys.RESOURCE, firstNonBlank(request.getHeader(HEADER_RESOURCE), request.getRequestURI()));
    putIfSafe(MdcKeys.OPERATION, firstNonBlank(request.getHeader(HEADER_OPERATION), request.getMethod()));
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

  public static boolean isSafeCorrelationId(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\0') >= 0) {
      return false;
    }
    return SAFE_CORRELATION_ID.matcher(value).matches();
  }

  private static void putIfSafe(String key, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    String trimmed = value.trim();
    if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\0') >= 0) {
      return;
    }
    if (SAFE_CONTEXT_TOKEN.matcher(trimmed).matches()) {
      MDC.put(key, trimmed);
    }
  }

  private static String resolveActor(HttpServletRequest request) {
    String headerActor = request.getHeader(HEADER_ACTOR);
    if (StringUtils.hasText(headerActor)) {
      return headerActor;
    }
    if (request.getUserPrincipal() != null && StringUtils.hasText(request.getUserPrincipal().getName())) {
      return request.getUserPrincipal().getName();
    }
    return null;
  }

  private static String firstNonBlank(String primary, String fallback) {
    if (StringUtils.hasText(primary)) {
      return primary;
    }
    return fallback;
  }

  /** Package-visible helper for tests. */
  static String normalizeForLog(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }
}
