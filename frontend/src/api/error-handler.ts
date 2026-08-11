import type { ProblemDetail } from './types'
import { AuthenticationError, ForbiddenError, HttpError } from './types'

const PROBLEM_CONTENT_TYPE = 'application/problem+json'

/**
 * Attempts to parse a Response body as RFC 9457 Problem Details.
 * Returns null if the Content-Type is not problem+json or the body is not valid JSON.
 */
export async function parseProblemDetail(response: Response): Promise<ProblemDetail | null> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (!contentType.includes(PROBLEM_CONTENT_TYPE)) {
    return null
  }
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return null
  }
}

/**
 * Extracts the X-Correlation-ID from a response or request, if present.
 */
export function extractCorrelationId(headers: Headers): string | null {
  return headers.get('X-Correlation-ID')
}

/**
 * Inspects an error HTTP response and throws the appropriate typed error.
 *
 * - 401: delegates to `onUnauthorized` (token refresh + redirect); throws AuthenticationError
 *        only if refresh fails. If `onUnauthorized` resolves without error, returns `true`
 *        to signal the caller to retry the original request once.
 * - 403: throws ForbiddenError immediately.
 * - Other 4xx/5xx: throws HttpError with parsed ProblemDetail if available.
 *
 * @returns `true` if the caller should retry the request (i.e. refresh succeeded).
 */
export async function handleErrorResponse(
  response: Response,
  onUnauthorized: (() => Promise<void>) | null,
): Promise<boolean> {
  const correlationId = extractCorrelationId(response.headers)
  const problem = await parseProblemDetail(response)

  if (response.status === 401) {
    if (onUnauthorized) {
      try {
        await onUnauthorized()
        return true // retry
      } catch {
        throw new AuthenticationError(problem, correlationId)
      }
    }
    throw new AuthenticationError(problem, correlationId)
  }

  if (response.status === 403) {
    throw new ForbiddenError(problem, correlationId)
  }

  throw new HttpError(response.status, response.statusText, problem, correlationId)
}
