package com.pcis.error;

/** Test fixtures that throw each supported exception type for MockMvc handler tests. */
public final class TestExceptions {

  public static final String ACTOR = "test-user";
  public static final String RESOURCE = "test/resource";
  public static final String OPERATION = "read";

  private TestExceptions() {}

  public static void throwResourceNotFound() {
    throw new ResourceNotFoundException("Customer not found", ACTOR, RESOURCE, OPERATION);
  }

  public static void throwValidation() {
    throw new ValidationException("Invalid field value", ACTOR, RESOURCE, OPERATION);
  }

  public static void throwAuthorizationDenied() {
    throw new AuthorizationDeniedException(
        "Missing linked approval", ACTOR, RESOURCE, OPERATION);
  }

  public static void throwConflict() {
    throw new ConflictException("Duplicate resource", ACTOR, RESOURCE, OPERATION);
  }

  public static void throwAuditWrite() {
    throw new AuditWriteException("Audit write failed", ACTOR, RESOURCE, OPERATION);
  }

  public static void throwNotImplemented() {
    throw new TerminalPcisException(
        ReasonCode.PRM_NOT_IMPLEMENTED,
        "Premium rating not implemented",
        ACTOR,
        RESOURCE,
        OPERATION);
  }

  public static void throwUnexpected() {
    throw new RuntimeException("secret internal detail");
  }
}
