package com.pcis.error;

import java.net.URI;

/** Versioned reason-code registry seeded from legacy message-file conventions. */
public enum ReasonCode {
  SYS_UNEXPECTED("SYS_UNEXPECTED", "Unexpected system error", URI.create("https://pcis.example/problems/unexpected")),
  SYS_VALIDATION("SYS_VALIDATION", "Request validation failed", URI.create("https://pcis.example/problems/validation")),
  SYS_NOT_FOUND("SYS_NOT_FOUND", "Resource not found", URI.create("https://pcis.example/problems/not-found")),
  SYS_CONFLICT("SYS_CONFLICT", "Resource conflict", URI.create("https://pcis.example/problems/conflict")),
  SYS_UNAUTHORIZED("SYS_UNAUTHORIZED", "Unauthenticated", URI.create("https://pcis.example/problems/unauthenticated")),
  SYS_FORBIDDEN("SYS_FORBIDDEN", "Forbidden", URI.create("https://pcis.example/problems/forbidden")),
  SYS_BUSINESS_RULE("SYS_BUSINESS_RULE", "Business rule rejected", URI.create("https://pcis.example/problems/business-rule")),
  AUD_WRITE_FAILURE("AUD_WRITE_FAILURE", "Audit write failed", URI.create("https://pcis.example/problems/audit-write-failure")),
  AUTHZ_DENIED_NO_APPROVAL("AUTHZ_DENIED_NO_APPROVAL", "Missing linked approval", URI.create("https://pcis.example/problems/missing-approval")),
  AUTHZ_LIMIT_EXCEEDED("AUTHZ_LIMIT_EXCEEDED", "Authority limit exceeded", URI.create("https://pcis.example/problems/authority-limit-exceeded")),
  PRM_NOT_IMPLEMENTED("PRM_NOT_IMPLEMENTED", "Premium rating not implemented", URI.create("https://pcis.example/problems/premium-not-implemented")),
  CFG_TUNABLE_NOT_FOUND("CFG_TUNABLE_NOT_FOUND", "Required tunable not found", URI.create("https://pcis.example/problems/tunable-not-found")),
  CFG_TUNABLE_OUT_OF_RANGE("CFG_TUNABLE_OUT_OF_RANGE", "Tunable value out of range", URI.create("https://pcis.example/problems/tunable-out-of-range")),
  CFG_UNKNOWN_CODE_VALUE("CFG_UNKNOWN_CODE_VALUE", "Unknown or inactive code value", URI.create("https://pcis.example/problems/unknown-code-value")),
  CFG_RULE_SET_NOT_FOUND("CFG_RULE_SET_NOT_FOUND", "Required rule set not found", URI.create("https://pcis.example/problems/rule-set-not-found"));

  private final String code;
  private final String title;
  private final URI type;

  ReasonCode(String code, String title, URI type) {
    this.code = code;
    this.title = title;
    this.type = type;
  }

  public String code() {
    return code;
  }

  public String title() {
    return title;
  }

  public URI type() {
    return type;
  }
}
