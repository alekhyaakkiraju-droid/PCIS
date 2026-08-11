package com.pcis.audit.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Validates v1 audit event field widths without silent truncation. */
public final class AuditEventValidator {

  public static final int MAX_ACTION = 10;
  public static final int MAX_OLD_NEW = 100;
  public static final int MAX_KEY = 40;
  public static final int MAX_SERVICE = 64;
  public static final int MAX_PROGRAM = 10;
  public static final int MAX_ACTOR = 10;
  public static final int MAX_RESOURCE = 50;
  public static final int MAX_FIELD_NAME = 30;

  private AuditEventValidator() {}

  public static ValidatedAuditEvent validate(AuditEventRequest request) {
    List<String> violations = new ArrayList<>();
    if (request == null) {
      violations.add("request body is required");
      throw new AuditValidationException(violations);
    }

    requireNonBlank(request.action(), "action", violations);
    requireMaxLength(request.action(), MAX_ACTION, "action", violations);
    requireMaxLength(request.oldValue(), MAX_OLD_NEW, "old_value", violations);
    requireMaxLength(request.newValue(), MAX_OLD_NEW, "new_value", violations);
    requireMaxLength(request.key(), MAX_KEY, "key", violations);
    requireNonBlank(request.service(), "service", violations);
    requireMaxLength(request.service(), MAX_SERVICE, "service", violations);
    requireMaxLength(request.program(), MAX_PROGRAM, "program", violations);
    requireNonBlank(request.actor(), "actor", violations);
    requireMaxLength(request.actor(), MAX_ACTOR, "actor", violations);
    requireNonBlank(request.resource(), "resource", violations);
    requireMaxLength(request.resource(), MAX_RESOURCE, "resource", violations);
    requireMaxLength(request.fieldName(), MAX_FIELD_NAME, "field_name", violations);

    AuditActionCode actionCode;
    try {
      actionCode = AuditActionCode.fromLegacy(request.action());
    } catch (UnknownAuditActionException ex) {
      violations.add("unknown action code: " + ex.legacyAction());
      throw new AuditValidationException(violations);
    }

    if (!violations.isEmpty()) {
      throw new AuditValidationException(violations);
    }

    UUID correlationId = request.correlationId() != null ? request.correlationId() : UUID.randomUUID();

    return new ValidatedAuditEvent(
        actionCode,
        request.oldValue(),
        request.newValue(),
        request.key(),
        correlationId,
        request.service(),
        request.program(),
        request.actor(),
        request.resource(),
        request.fieldName(),
        actionCode.operation());
  }

  private static void requireNonBlank(String value, String field, List<String> violations) {
    if (value == null || value.isBlank()) {
      violations.add(field + " is required");
    }
  }

  private static void requireMaxLength(String value, int max, String field, List<String> violations) {
    if (value != null && value.length() > max) {
      violations.add(field + " exceeds maximum length of " + max + " (got " + value.length() + ")");
    }
  }
}
