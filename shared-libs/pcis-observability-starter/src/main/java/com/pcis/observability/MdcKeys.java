package com.pcis.observability;

/**
 * Canonical MDC keys emitted by the PCIS observability starter.
 */
public final class MdcKeys {

  public static final String CORRELATION_ID = "correlationId";
  public static final String SERVICE = "service";
  public static final String PROGRAM = "program";
  public static final String ACTOR = "actor";
  public static final String RESOURCE = "resource";
  public static final String OPERATION = "operation";
  public static final String JOB_ID = "jobId";
  public static final String RUN_ID = "runId";

  private MdcKeys() {}
}
