import { describe, it, expect, vi } from 'vitest'
import { parseProblemDetail, extractCorrelationId, handleErrorResponse } from '../error-handler'
import { AuthenticationError, ForbiddenError, HttpError } from '../types'

function makeProblemResponse(status: number, body: object, correlationId?: string): Response {
  const headers: Record<string, string> = {
    'Content-Type': 'application/problem+json',
  }
  if (correlationId) {
    headers['X-Correlation-ID'] = correlationId
  }
  return new Response(JSON.stringify(body), { status, headers })
}

function makeJsonResponse(status: number, body: object, correlationId?: string): Response {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (correlationId) {
    headers['X-Correlation-ID'] = correlationId
  }
  return new Response(JSON.stringify(body), { status, headers })
}

describe('parseProblemDetail', () => {
  it('parses a valid RFC 9457 application/problem+json response', async () => {
    const body = {
      type: 'https://pcis.example/problems/not-found',
      title: 'Not Found',
      status: 404,
      detail: 'Claim CLM000001 does not exist',
      instance: '/api/v1/claims/CLM000001',
    }
    const response = makeProblemResponse(404, body)
    const result = await parseProblemDetail(response)
    expect(result).not.toBeNull()
    expect(result?.detail).toBe('Claim CLM000001 does not exist')
    expect(result?.type).toBe('https://pcis.example/problems/not-found')
    expect(result?.status).toBe(404)
  })

  it('returns null for application/json Content-Type', async () => {
    const response = makeJsonResponse(400, { error: 'bad request' })
    const result = await parseProblemDetail(response)
    expect(result).toBeNull()
  })

  it('returns null when Content-Type header is absent', async () => {
    const response = new Response(JSON.stringify({ detail: 'error' }), { status: 400 })
    const result = await parseProblemDetail(response)
    expect(result).toBeNull()
  })

  it('returns null when body is not valid JSON', async () => {
    const response = new Response('not valid json { unclosed', {
      status: 500,
      headers: { 'Content-Type': 'application/problem+json' },
    })
    const result = await parseProblemDetail(response)
    expect(result).toBeNull()
  })

  it('handles Content-Type with charset suffix: application/problem+json; charset=utf-8', async () => {
    const body = { status: 400, title: 'Bad Request', detail: 'Missing field' }
    const response = new Response(JSON.stringify(body), {
      status: 400,
      headers: { 'Content-Type': 'application/problem+json; charset=utf-8' },
    })
    const result = await parseProblemDetail(response)
    expect(result).not.toBeNull()
    expect(result?.detail).toBe('Missing field')
  })

  it('preserves extension fields (e.g. invalid-params for validation errors)', async () => {
    const body = {
      type: 'https://pcis.example/problems/validation',
      title: 'Validation Error',
      status: 422,
      detail: 'One or more fields are invalid',
      'invalid-params': [{ name: 'paymentAmt', reason: 'Must be positive' }],
    }
    const response = makeProblemResponse(422, body)
    const result = await parseProblemDetail(response)
    expect(result).not.toBeNull()
    expect((result as Record<string, unknown>)['invalid-params']).toHaveLength(1)
  })
})

describe('extractCorrelationId', () => {
  it('returns the X-Correlation-ID header value', () => {
    const headers = new Headers({ 'X-Correlation-ID': 'abc-123' })
    expect(extractCorrelationId(headers)).toBe('abc-123')
  })

  it('returns null when header is absent', () => {
    const headers = new Headers({ 'Content-Type': 'application/json' })
    expect(extractCorrelationId(headers)).toBeNull()
  })

  it('is case-insensitive (headers are case-insensitive by spec)', () => {
    const headers = new Headers({ 'x-correlation-id': 'lower-case' })
    expect(extractCorrelationId(headers)).toBe('lower-case')
  })
})

describe('handleErrorResponse', () => {
  describe('HTTP 401', () => {
    it('calls onUnauthorized and returns true (retry signal)', async () => {
      const onUnauthorized = vi.fn().mockResolvedValue(undefined)
      const response = makeProblemResponse(401, { status: 401, detail: 'Token expired' })
      const shouldRetry = await handleErrorResponse(response, onUnauthorized)
      expect(onUnauthorized).toHaveBeenCalledOnce()
      expect(shouldRetry).toBe(true)
    })

    it('throws AuthenticationError when onUnauthorized rejects', async () => {
      const onUnauthorized = vi.fn().mockRejectedValue(new Error('Refresh token invalid'))
      const response = makeProblemResponse(401, { status: 401, detail: 'Token expired' })
      await expect(handleErrorResponse(response, onUnauthorized)).rejects.toBeInstanceOf(
        AuthenticationError,
      )
    })

    it('throws AuthenticationError when no onUnauthorized callback provided', async () => {
      const response = makeProblemResponse(401, { status: 401, detail: 'Unauthorized' })
      await expect(handleErrorResponse(response, null)).rejects.toBeInstanceOf(AuthenticationError)
    })

    it('propagates correlationId into AuthenticationError', async () => {
      const onUnauthorized = vi.fn().mockRejectedValue(new Error('fail'))
      const response = makeProblemResponse(
        401,
        { status: 401 },
        'trace-id-xyz',
      )
      const err = await handleErrorResponse(response, onUnauthorized).catch((e: unknown) => e)
      expect((err as AuthenticationError).correlationId).toBe('trace-id-xyz')
    })
  })

  describe('HTTP 403', () => {
    it('throws ForbiddenError', async () => {
      const response = makeProblemResponse(403, {
        status: 403,
        title: 'Forbidden',
        detail: 'Insufficient authority level',
      })
      await expect(handleErrorResponse(response, null)).rejects.toBeInstanceOf(ForbiddenError)
    })

    it('includes ProblemDetail in ForbiddenError', async () => {
      const response = makeProblemResponse(403, {
        status: 403,
        detail: 'Adjuster cannot approve claims above authority limit',
      })
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect((err as ForbiddenError).problem?.detail).toBe(
        'Adjuster cannot approve claims above authority limit',
      )
    })

    it('propagates correlationId into ForbiddenError', async () => {
      const response = makeProblemResponse(403, { status: 403 }, 'corr-abc-789')
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect((err as ForbiddenError).correlationId).toBe('corr-abc-789')
    })
  })

  describe('other 4xx / 5xx', () => {
    it('throws HttpError for 400', async () => {
      const response = makeProblemResponse(400, { status: 400, detail: 'Malformed request' })
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect(err).toBeInstanceOf(HttpError)
      expect((err as HttpError).status).toBe(400)
    })

    it('throws HttpError for 404', async () => {
      const response = makeProblemResponse(404, { status: 404, detail: 'Not found' })
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect(err).toBeInstanceOf(HttpError)
      expect((err as HttpError).status).toBe(404)
    })

    it('throws HttpError for 422', async () => {
      const response = makeProblemResponse(422, {
        status: 422,
        detail: 'paymentAmt must be positive',
      })
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect(err).toBeInstanceOf(HttpError)
      expect((err as HttpError).status).toBe(422)
    })

    it('throws HttpError for 500 with null problem when not problem+json', async () => {
      const response = new Response('Internal Server Error', {
        status: 500,
        headers: { 'Content-Type': 'text/plain' },
      })
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect(err).toBeInstanceOf(HttpError)
      expect((err as HttpError).status).toBe(500)
      expect((err as HttpError).problem).toBeNull()
    })

    it('throws HttpError for 502 (included for completeness — handled via retry first)', async () => {
      const response = makeProblemResponse(502, { status: 502, detail: 'Bad gateway' })
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect(err).toBeInstanceOf(HttpError)
      expect((err as HttpError).status).toBe(502)
    })

    it('preserves correlationId from response header', async () => {
      const response = makeProblemResponse(400, { status: 400 }, 'trace-400')
      const err = await handleErrorResponse(response, null).catch((e: unknown) => e)
      expect((err as HttpError).correlationId).toBe('trace-400')
    })
  })
})
