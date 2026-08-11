package com.pcis.observability.propagation;

/**
 * Canonical header names for cross-service correlation propagation.
 */
public final class ObservabilityHeaders {

  /** HTTP request/response header (see {@link com.pcis.observability.filter.CorrelationIdFilter}). */
  public static final String HTTP_CORRELATION_ID = "X-Correlation-ID";

  /** Kafka record header emitted by {@code pcis-outbox} producers. */
  public static final String KAFKA_CORRELATION_ID = "pcis-correlation-id";

  private ObservabilityHeaders() {}
}
