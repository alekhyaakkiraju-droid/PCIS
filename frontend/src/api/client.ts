import { trace } from '@opentelemetry/api'
import { ConnectionError } from './types'
import { withRetry, DEFAULT_RETRY_CONFIG, type RetryConfig } from './retry'
import { handleErrorResponse } from './error-handler'

const CORRELATION_HEADER = 'X-Correlation-ID'
const CONTENT_TYPE_JSON = 'application/json'

function getCorrelationId(): string {
  const span = trace.getActiveSpan()
  if (span) {
    return span.spanContext().traceId
  }
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return (crypto as Crypto).randomUUID()
  }
  return `corr-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

export interface ApiClientConfig {
  /** Base URL prepended to all request paths. Defaults to VITE_API_BASE_URL ?? '/api'. */
  baseUrl?: string
  /**
   * Called on HTTP 401. Should attempt token refresh.
   * Resolve to allow retry; throw to signal refresh failed.
   */
  onUnauthorized?: () => Promise<void>
  /**
   * Called on HTTP 403 after ForbiddenError is thrown.
   * Typical use: navigate to /forbidden page.
   */
  onForbidden?: (path: string) => void
  /** Override retry configuration (useful in tests). */
  retryConfig?: RetryConfig
}

/**
 * Centralized HTTP client for all PCIS domain service calls.
 *
 * Features:
 * - Automatic X-Correlation-ID header on every request
 * - `credentials: 'include'` for httpOnly cookie auth (BFF pattern)
 * - RFC 9457 Problem Details parsing
 * - Exponential backoff retry for 502/503/504 (GET only)
 * - Token refresh deduplication on 401
 * - Typed error hierarchy (ConnectionError, HttpError, ForbiddenError, AuthenticationError)
 */
export class ApiClient {
  private readonly baseUrl: string
  private readonly retryConfig: RetryConfig
  private onUnauthorized: (() => Promise<void>) | null
  private onForbidden: ((path: string) => void) | null

  // Deduplicate concurrent 401 refresh attempts — only one in-flight at a time.
  private refreshPromise: Promise<void> | null = null

  constructor(config: ApiClientConfig = {}) {
    this.baseUrl =
      config.baseUrl ??
      (typeof import.meta !== 'undefined'
        ? (import.meta.env?.VITE_API_BASE_URL ?? '/api')
        : '/api')
    this.onUnauthorized = config.onUnauthorized ?? null
    this.onForbidden = config.onForbidden ?? null
    this.retryConfig = config.retryConfig ?? DEFAULT_RETRY_CONFIG
  }

  /**
   * Updates auth callbacks after initial construction.
   * Called from the React app once AuthProvider is mounted.
   */
  configure(config: Pick<ApiClientConfig, 'onUnauthorized' | 'onForbidden'>): void {
    if (config.onUnauthorized !== undefined) {
      this.onUnauthorized = config.onUnauthorized
    }
    if (config.onForbidden !== undefined) {
      this.onForbidden = config.onForbidden
    }
  }

  /**
   * Performs an HTTP request with automatic retry, error handling, and auth refresh.
   *
   * @param method - HTTP method
   * @param path - Path relative to baseUrl (e.g. '/v1/customers')
   * @param options - Optional RequestInit overrides and typed request body
   * @returns Parsed response body as T (undefined for 204 No Content)
   */
  async request<T = unknown>(
    method: string,
    path: string,
    options: RequestInit & { body?: unknown } = {},
  ): Promise<T> {
    const correlationId = getCorrelationId()
    const url = `${this.baseUrl}${path}`

    const buildInit = (): RequestInit => {
      const headers = new Headers(options.headers)
      headers.set(CORRELATION_HEADER, correlationId)
      headers.set('Accept', CONTENT_TYPE_JSON)
      if (options.body !== undefined && options.body !== null) {
        headers.set('Content-Type', CONTENT_TYPE_JSON)
      }
      return {
        ...options,
        method,
        headers,
        credentials: 'include',
        body:
          options.body !== undefined && options.body !== null
            ? JSON.stringify(options.body)
            : options.body as BodyInit | null | undefined,
      }
    }

    const doFetch = (): Promise<Response> =>
      fetch(url, buildInit()).catch((cause: unknown) => {
        throw new ConnectionError(cause, correlationId)
      })

    let response = await withRetry(doFetch, method, this.retryConfig)

    if (!response.ok) {
      const refresher = this.onUnauthorized
        ? () => {
            if (!this.refreshPromise) {
              this.refreshPromise = this.onUnauthorized!().finally(() => {
                this.refreshPromise = null
              })
            }
            return this.refreshPromise
          }
        : null

      try {
        const shouldRetry = await handleErrorResponse(response, refresher)
        if (shouldRetry) {
          response = await fetch(url, buildInit()).catch((cause: unknown) => {
            throw new ConnectionError(cause, correlationId)
          })
          if (!response.ok) {
            await handleErrorResponse(response, null)
          }
        }
      } catch (error: unknown) {
        if (error instanceof Error && error.name === 'ForbiddenError') {
          this.onForbidden?.(path)
        }
        throw error
      }
    }

    // 204 No Content — do not attempt JSON parsing
    if (response.status === 204) {
      return undefined as T
    }

    return (await response.json()) as T
  }

  /** Convenience wrappers */
  get<T = unknown>(path: string, options?: RequestInit): Promise<T> {
    return this.request<T>('GET', path, options)
  }

  post<T = unknown>(path: string, body?: unknown, options?: RequestInit): Promise<T> {
    return this.request<T>('POST', path, { ...options, body } as RequestInit & { body?: unknown })
  }

  put<T = unknown>(path: string, body?: unknown, options?: RequestInit): Promise<T> {
    return this.request<T>('PUT', path, { ...options, body } as RequestInit & { body?: unknown })
  }

  patch<T = unknown>(path: string, body?: unknown, options?: RequestInit): Promise<T> {
    return this.request<T>('PATCH', path, { ...options, body } as RequestInit & { body?: unknown })
  }

  delete<T = unknown>(path: string, options?: RequestInit): Promise<T> {
    return this.request<T>('DELETE', path, options)
  }
}

/** Application-wide singleton — configure once at app startup via apiClient.configure() */
export const apiClient = new ApiClient()
