package com.pcis.error;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;

public final class ProblemDetailFactory {

  private static final URI DEFAULT_TYPE = URI.create("https://pcis.example/problems/pcis-error");

  private ProblemDetailFactory() {}

  public static PcisProblemDetail fromReason(
      ReasonCode reason,
      HttpStatus status,
      String detail,
      URI instance,
      String correlationId,
      List<ProblemErrorEntry> errors) {
    return PcisProblemDetail.builder()
        .type(reason.type())
        .title(reason.title())
        .status(status.value())
        .detail(detail)
        .instance(instance)
        .code(reason.code())
        .correlationId(correlationId)
        .errors(errors == null || errors.isEmpty() ? null : List.copyOf(errors))
        .build();
  }

  public static PcisProblemDetail unexpected(
      String detail, URI instance, String correlationId) {
    return fromReason(
        ReasonCode.SYS_UNEXPECTED,
        HttpStatus.INTERNAL_SERVER_ERROR,
        detail,
        instance,
        correlationId,
        null);
  }

  public static PcisProblemDetail notImplemented(
      ReasonCode reason, URI instance, String correlationId) {
    return fromReason(
        reason,
        HttpStatus.NOT_IMPLEMENTED,
        reason.title(),
        instance,
        correlationId,
        null);
  }
}
