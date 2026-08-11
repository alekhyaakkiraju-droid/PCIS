type MockResponse = {
  status?: number
  body: unknown
}

/**
 * Test utility for mocking fetch in Vitest.
 */
export function createMockFetch(responses: Record<string, MockResponse>): typeof fetch {
  return viFetch(async (input: RequestInfo | URL) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
    const match = Object.entries(responses).find(([pattern]) => url.includes(pattern))
    if (!match) {
      return new Response(JSON.stringify({ error: 'not mocked', url }), {
        status: 404,
        headers: { 'Content-Type': 'application/json' },
      })
    }
    const [, response] = match
    return new Response(JSON.stringify(response.body), {
      status: response.status ?? 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function viFetch(impl: typeof fetch): typeof fetch {
  return impl
}
