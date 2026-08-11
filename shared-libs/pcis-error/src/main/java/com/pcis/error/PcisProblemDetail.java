package com.pcis.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;

/** Canonical RFC 9457 problem detail with PCIS extensions. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PcisProblemDetail(
    URI type,
    String title,
    int status,
    String detail,
    URI instance,
    String code,
    @JsonProperty("correlation_id") String correlationId,
    List<ProblemErrorEntry> errors) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private String code;
    private String correlationId;
    private List<ProblemErrorEntry> errors;

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

    public Builder code(String code) {
      this.code = code;
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder errors(List<ProblemErrorEntry> errors) {
      this.errors = errors;
      return this;
    }

    public PcisProblemDetail build() {
      return new PcisProblemDetail(type, title, status, detail, instance, code, correlationId, errors);
    }
  }
}
