/**
 * Shared types for the PCIS API client layer.
 * All errors include correlationId for distributed tracing.
 */

/** RFC 9457 Problem Details for HTTP APIs */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  /** Service-specific extension fields */
  [key: string]: unknown
}

/** Thrown for non-retryable HTTP errors (4xx, non-502/503/504 5xx) */
export class HttpError extends Error {
  constructor(
    public readonly status: number,
    public readonly statusText: string,
    public readonly problem: ProblemDetail | null,
    public readonly correlationId: string | null,
  ) {
    super(
      problem?.detail ??
        problem?.title ??
        `HTTP ${status}: ${statusText}`,
    )
    this.name = 'HttpError'
  }
}

/** Thrown for network-level failures (offline, DNS, timeout) */
export class ConnectionError extends Error {
  constructor(
    public readonly cause: unknown,
    public readonly correlationId: string | null,
  ) {
    super(
      cause instanceof Error
        ? `Network error: ${cause.message}`
        : 'Network error: connection failed',
    )
    this.name = 'ConnectionError'
  }
}

/** Thrown when a request is rejected with HTTP 403 Forbidden */
export class ForbiddenError extends HttpError {
  constructor(problem: ProblemDetail | null, correlationId: string | null) {
    super(403, 'Forbidden', problem, correlationId)
    this.name = 'ForbiddenError'
  }
}

/** Thrown when a request is rejected with HTTP 401 and token refresh has failed */
export class AuthenticationError extends HttpError {
  constructor(problem: ProblemDetail | null, correlationId: string | null) {
    super(401, 'Unauthorized', problem, correlationId)
    this.name = 'AuthenticationError'
  }
}
