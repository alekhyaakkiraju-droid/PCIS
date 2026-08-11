import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { withRetry, DEFAULT_RETRY_CONFIG, type RetryConfig } from '../retry'

function makeResponse(status: number): Response {
  return new Response(null, { status })
}

describe('withRetry', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const INSTANT_CONFIG: RetryConfig = {
    maxAttempts: 3,
    backoffMs: [0, 0, 0],
    retryStatusCodes: new Set([502, 503, 504]),
    nonRetryableMethods: new Set(['POST', 'PUT', 'DELETE', 'PATCH']),
  }

  describe('retryable methods (GET)', () => {
    it('returns on first success without retrying', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(200))
      const response = await withRetry(fn, 'GET', INSTANT_CONFIG)
      expect(response.status).toBe(200)
      expect(fn).toHaveBeenCalledOnce()
    })

    it('retries on 502 and returns 200 on second attempt', async () => {
      const fn = vi
        .fn()
        .mockResolvedValueOnce(makeResponse(502))
        .mockResolvedValueOnce(makeResponse(200))
      const responsePromise = withRetry(fn, 'GET', INSTANT_CONFIG)
      await vi.runAllTimersAsync()
      const response = await responsePromise
      expect(response.status).toBe(200)
      expect(fn).toHaveBeenCalledTimes(2)
    })

    it('retries on 503 and returns 200 on second attempt', async () => {
      const fn = vi
        .fn()
        .mockResolvedValueOnce(makeResponse(503))
        .mockResolvedValueOnce(makeResponse(200))
      const responsePromise = withRetry(fn, 'GET', INSTANT_CONFIG)
      await vi.runAllTimersAsync()
      const response = await responsePromise
      expect(response.status).toBe(200)
      expect(fn).toHaveBeenCalledTimes(2)
    })

    it('retries on 504 and returns 200 on second attempt', async () => {
      const fn = vi
        .fn()
        .mockResolvedValueOnce(makeResponse(504))
        .mockResolvedValueOnce(makeResponse(200))
      const responsePromise = withRetry(fn, 'GET', INSTANT_CONFIG)
      await vi.runAllTimersAsync()
      const response = await responsePromise
      expect(response.status).toBe(200)
      expect(fn).toHaveBeenCalledTimes(2)
    })

    it('exhausts maxAttempts and returns final failed response', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(502))
      const responsePromise = withRetry(fn, 'GET', INSTANT_CONFIG)
      await vi.runAllTimersAsync()
      const response = await responsePromise
      expect(response.status).toBe(502)
      expect(fn).toHaveBeenCalledTimes(3)
    })

    it('does NOT retry on 400', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(400))
      const response = await withRetry(fn, 'GET', INSTANT_CONFIG)
      expect(response.status).toBe(400)
      expect(fn).toHaveBeenCalledOnce()
    })

    it('does NOT retry on 401', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(401))
      const response = await withRetry(fn, 'GET', INSTANT_CONFIG)
      expect(response.status).toBe(401)
      expect(fn).toHaveBeenCalledOnce()
    })

    it('does NOT retry on 403', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(403))
      const response = await withRetry(fn, 'GET', INSTANT_CONFIG)
      expect(response.status).toBe(403)
      expect(fn).toHaveBeenCalledOnce()
    })

    it('does NOT retry on 404', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(404))
      const response = await withRetry(fn, 'GET', INSTANT_CONFIG)
      expect(response.status).toBe(404)
      expect(fn).toHaveBeenCalledOnce()
    })

    it('does NOT retry on 500 (not in retryStatusCodes)', async () => {
      const fn = vi.fn().mockResolvedValue(makeResponse(500))
      const response = await withRetry(fn, 'GET', INSTANT_CONFIG)
      expect(response.status).toBe(500)
      expect(fn).toHaveBeenCalledOnce()
    })
  })

  describe('non-idempotent methods', () => {
    it.each(['POST', 'PUT', 'DELETE', 'PATCH'])(
      'makes exactly 1 attempt for %s even on 502',
      async (method) => {
        const fn = vi.fn().mockResolvedValue(makeResponse(502))
        const response = await withRetry(fn, method, INSTANT_CONFIG)
        expect(response.status).toBe(502)
        expect(fn).toHaveBeenCalledOnce()
      },
    )
  })

  describe('backoff timing', () => {
    it('applies delay between retry attempts', async () => {
      const TIMED_CONFIG: RetryConfig = {
        maxAttempts: 3,
        backoffMs: [1000, 2000, 4000],
        retryStatusCodes: new Set([502]),
        nonRetryableMethods: new Set(['POST', 'PUT', 'DELETE', 'PATCH']),
      }

      const fn = vi
        .fn()
        .mockResolvedValueOnce(makeResponse(502))
        .mockResolvedValueOnce(makeResponse(502))
        .mockResolvedValueOnce(makeResponse(200))

      const promise = withRetry(fn, 'GET', TIMED_CONFIG)

      // After first call fails, should wait 1000ms before second attempt
      expect(fn).toHaveBeenCalledTimes(1)
      await vi.advanceTimersByTimeAsync(1000)
      expect(fn).toHaveBeenCalledTimes(2)

      // After second call fails, should wait 2000ms before third attempt
      await vi.advanceTimersByTimeAsync(2000)
      expect(fn).toHaveBeenCalledTimes(3)

      const response = await promise
      expect(response.status).toBe(200)
    })

    it('does not add delay before first attempt', async () => {
      const TIMED_CONFIG: RetryConfig = {
        maxAttempts: 2,
        backoffMs: [5000],
        retryStatusCodes: new Set([502]),
        nonRetryableMethods: new Set(['POST']),
      }

      const fn = vi
        .fn()
        .mockResolvedValueOnce(makeResponse(502))
        .mockResolvedValueOnce(makeResponse(200))

      const promise = withRetry(fn, 'GET', TIMED_CONFIG)
      // Immediately after call: first attempt should have fired
      expect(fn).toHaveBeenCalledTimes(1)

      // Only after advancing 5000ms does the second fire
      await vi.advanceTimersByTimeAsync(5000)
      const response = await promise
      expect(response.status).toBe(200)
    })
  })

  describe('DEFAULT_RETRY_CONFIG', () => {
    it('retries status codes 502, 503, 504', () => {
      expect(DEFAULT_RETRY_CONFIG.retryStatusCodes.has(502)).toBe(true)
      expect(DEFAULT_RETRY_CONFIG.retryStatusCodes.has(503)).toBe(true)
      expect(DEFAULT_RETRY_CONFIG.retryStatusCodes.has(504)).toBe(true)
    })

    it('marks POST, PUT, DELETE, PATCH as non-retryable', () => {
      const methods = ['POST', 'PUT', 'DELETE', 'PATCH']
      for (const m of methods) {
        expect(DEFAULT_RETRY_CONFIG.nonRetryableMethods.has(m)).toBe(true)
      }
    })

    it('has 3 max attempts with 1s/2s/4s backoff', () => {
      expect(DEFAULT_RETRY_CONFIG.maxAttempts).toBe(3)
      expect(DEFAULT_RETRY_CONFIG.backoffMs).toEqual([1000, 2000, 4000])
    })
  })
})
