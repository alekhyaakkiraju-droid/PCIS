import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ProtectedRoute } from './ProtectedRoute'
import { authFixtures } from '../test-fixtures/authSessions'

const mockLogin = vi.fn()

vi.mock('./AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./AuthContext')>()
  return {
    ...actual,
    useAuth: () => ({
      status: mockAuthState.status,
      user: mockAuthState.user,
      login: mockLogin,
      logout: vi.fn(),
      refreshSession: vi.fn(),
      hasRole: (role: string) => mockAuthState.user?.roles.includes(role as never) ?? false,
    }),
  }
})

const mockAuthState: {
  status: 'loading' | 'authenticated' | 'unauthenticated'
  user: (typeof authFixtures.adjuster)['user'] | null
} = {
  status: 'loading',
  user: null,
}

function ProtectedClaims() {
  return (
    <ProtectedRoute>
      <p>Claims workspace</p>
    </ProtectedRoute>
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    mockLogin.mockReset()
    mockAuthState.status = 'loading'
    mockAuthState.user = null
  })

  it('shows loading state while session resolves', () => {
    mockAuthState.status = 'loading'
    render(
      <MemoryRouter initialEntries={['/claims']}>
        <Routes>
          <Route path="/claims" element={<ProtectedClaims />} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByRole('status')).toHaveTextContent('Checking session')
  })

  it('triggers login redirect when unauthenticated', async () => {
    mockAuthState.status = 'unauthenticated'
    render(
      <MemoryRouter initialEntries={['/claims']}>
        <Routes>
          <Route path="/claims" element={<ProtectedClaims />} />
        </Routes>
      </MemoryRouter>,
    )
    await waitFor(() => expect(mockLogin).toHaveBeenCalledWith('/claims'))
    expect(screen.getByRole('status')).toHaveTextContent('Redirecting to sign in')
  })

  it('allows authenticated users through regardless of route RBAC', () => {
    mockAuthState.status = 'authenticated'
    mockAuthState.user = authFixtures.csr.user
    render(
      <MemoryRouter initialEntries={['/claims']}>
        <Routes>
          <Route path="/claims" element={<ProtectedClaims />} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('Claims workspace')).toBeInTheDocument()
  })
})
