package com.pcis.gateway.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.util.Map;

/** RFC 9457 problem detail payload. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
    URI type,
    String title,
    int status,
    String detail,
    URI instance,
    String correlationId,
    Map<String, Object> extensions) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private String correlationId;
    private Map<String, Object> extensions;

    public Builder type(URI type) {
      this.type = type;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder status(int status) {
      this.status = status;
      return this;
    }

    public Builder detail(String detail) {
      this.detail = detail;
      return this;
    }

    public Builder instance(URI instance) {
      this.instance = instance;
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder extensions(Map<String, Object> extensions) {
      this.extensions = extensions;
      return this;
    }

    public ProblemDetail build() {
      return new ProblemDetail(type, title, status, detail, instance, correlationId, extensions);
    }
  }
}
