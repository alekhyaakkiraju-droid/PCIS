import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { UserManager } from 'oidc-client-ts'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'
import { authFixtures } from '../test-fixtures/authSessions'

const mockSigninRedirect = vi.fn()
const mockSignoutRedirect = vi.fn()
const mockRemoveUser = vi.fn()

function createMockUserManager() {
  return {
    signinRedirect: mockSigninRedirect,
    signoutRedirect: mockSignoutRedirect,
    removeUser: mockRemoveUser,
  } as unknown as UserManager
}

function Probe() {
  const { status, user, login, logout, hasRole } = useAuth()
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="user">{user?.name ?? 'none'}</span>
      <span data-testid="has-adjuster">{String(hasRole('CLAIMS_ADJUSTER'))}</span>
      <button type="button" onClick={() => void login('/claims')}>
        Login
      </button>
      <button type="button" onClick={() => void logout()}>
        Logout
      </button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    mockSigninRedirect.mockReset()
    mockSignoutRedirect.mockReset()
    mockRemoveUser.mockReset()
    sessionStorage.clear()
  })

  it('loads session from /api/auth/session with credentials', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(authFixtures.adjuster), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MemoryRouter>
        <AuthProvider userManager={createMockUserManager()}>
          <Probe />
        </AuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(screen.getByTestId('user')).toHaveTextContent('Alice Adjuster')
    expect(screen.getByTestId('has-adjuster')).toHaveTextContent('true')
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/session', expect.objectContaining({
      credentials: 'include',
    }))
  })

  it('starts OIDC signinRedirect on login', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(authFixtures.unauthenticated), {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AuthProvider userManager={createMockUserManager()}>
          <Probe />
        </AuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))
    await user.click(screen.getByRole('button', { name: 'Login' }))
    await waitFor(() => expect(mockSigninRedirect).toHaveBeenCalledWith({ state: '/claims' }))
    expect(sessionStorage.getItem('pcis.auth.returnUrl')).toBe('/claims')
  })

  it('clears session and calls signoutRedirect on logout', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify(authFixtures.adjuster), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    vi.stubGlobal('fetch', fetchMock)

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AuthProvider userManager={createMockUserManager()}>
          <Probe />
        </AuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    await user.click(screen.getByRole('button', { name: 'Logout' }))

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))
    expect(mockRemoveUser).toHaveBeenCalled()
    expect(mockSignoutRedirect).toHaveBeenCalled()
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/logout', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
    }))
  })
})
