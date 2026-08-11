/** HTTP status codes that indicate a transient backend or gateway error worth retrying. */
export const RETRYABLE_STATUS_CODES: ReadonlySet<number> = new Set([502, 503, 504])

/**
 * HTTP methods that MUST NOT be retried to avoid duplicate mutations.
 * Only GET (and HEAD/OPTIONS) are considered idempotent here.
 */
export const NON_RETRYABLE_METHODS: ReadonlySet<string> = new Set([
  'POST',
  'PUT',
  'DELETE',
  'PATCH',
])

export interface RetryConfig {
  maxAttempts: number
  backoffMs: readonly number[]
  retryStatusCodes: ReadonlySet<number>
  nonRetryableMethods: ReadonlySet<string>
}

export const DEFAULT_RETRY_CONFIG: RetryConfig = {
  maxAttempts: 3,
  backoffMs: [1000, 2000, 4000],
  retryStatusCodes: RETRYABLE_STATUS_CODES,
  nonRetryableMethods: NON_RETRYABLE_METHODS,
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Wraps an async fetch call with exponential backoff retry.
 *
 * Retry is skipped entirely for non-idempotent HTTP methods (POST/PUT/DELETE/PATCH)
 * to prevent duplicate mutations. Retries happen only on transient 502/503/504 errors.
 *
 * @param fn - Zero-argument factory that produces a fetch promise.
 * @param method - HTTP method of the request (used to gate retry eligibility).
 * @param config - Optional override of retry configuration (useful in tests for zero delays).
 * @returns The last successful or non-retryable response.
 * @throws The last response error if all retries are exhausted.
 */
export async function withRetry(
  fn: () => Promise<Response>,
  method: string,
  config: RetryConfig = DEFAULT_RETRY_CONFIG,
): Promise<Response> {
  const upperMethod = method.toUpperCase()
  const isIdempotent = !config.nonRetryableMethods.has(upperMethod)

  let lastResponse: Response | null = null
  const attempts = isIdempotent ? config.maxAttempts : 1

  for (let attempt = 0; attempt < attempts; attempt++) {
    lastResponse = await fn()

    if (!config.retryStatusCodes.has(lastResponse.status)) {
      return lastResponse
    }

    const isLastAttempt = attempt === attempts - 1
    if (!isLastAttempt) {
      const waitMs = config.backoffMs[attempt] ?? config.backoffMs[config.backoffMs.length - 1]
      await delay(waitMs ?? 0)
    }
  }

  // All retry attempts exhausted — return the last transient-error response for
  // the caller (ApiClient) to turn into a structured error.
  return lastResponse!
}
