import { render, screen, waitFor } from '@testing-library/react'
import type { UserManager } from 'oidc-client-ts'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AuthProvider } from './AuthContext'
import { LoginCallback } from '../pages/LoginCallback'
import { authFixtures } from '../test-fixtures/authSessions'

const mockNavigate = vi.fn()

vi.mock('react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router')>()
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

function createMockUserManager() {
  return {
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  } as unknown as UserManager
}

describe('OIDC login callback integration', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    sessionStorage.clear()
  })

  it('posts callback code to BFF and navigates to return route', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
      if (url.includes('/api/auth/callback') && init?.method === 'POST') {
        return new Response(null, { status: 204 })
      }
      if (url.includes('/api/auth/session')) {
        return new Response(JSON.stringify(authFixtures.adjuster), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response(JSON.stringify({ error: 'not mocked' }), { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter initialEntries={['/auth/callback?code=abc123&state=/claims']}>
        <AuthProvider userManager={createMockUserManager()}>
          <Routes>
            <Route path="/auth/callback" element={<LoginCallback />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/auth/callback',
        expect.objectContaining({
          method: 'POST',
          credentials: 'include',
          body: JSON.stringify({ code: 'abc123', state: '/claims' }),
        }),
      ),
    )

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/claims', { replace: true }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
