import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '@/auth/AuthContext'
import { DemoRoleProvider } from '@/demo/demo-role'
import { useCapabilities } from './useCapabilities'

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(() => ({
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  })),
  WebStorageStateStore: vi.fn(),
}))

function wrapper(session: object) {
  return ({ children }: { children: React.ReactNode }) => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(session), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    return (
      <AuthProvider>
        <DemoRoleProvider>{children}</DemoRoleProvider>
      </AuthProvider>
    )
  }
}

describe('useCapabilities', () => {
  it('exposes adjuster claims access', async () => {
    const { result } = renderHook(() => useCapabilities(), {
      wrapper: wrapper({
        authenticated: true,
        user: {
          sub: 'adj',
          name: 'Adj',
          email: 'a@x.com',
          roles: ['CLAIMS_ADJUSTER'],
          authority_limit: 25000,
        },
      }),
    })
    await waitFor(() => expect(result.current.roles).toEqual(['CLAIMS_ADJUSTER']))
    expect(result.current.canAccessRoute('/claims')).toBe(true)
    expect(result.current.canAccessRoute('/customers')).toBe(false)
    expect(result.current.hasPermission('claims:write')).toBe(true)
    expect(result.current.authorityLimit).toBe(25000)
  })

  it('exposes finance billing access only', async () => {
    const { result } = renderHook(() => useCapabilities(), {
      wrapper: wrapper({
        authenticated: true,
        user: { sub: 'fin', name: 'Fin', email: 'f@x.com', roles: ['FINANCE'] },
      }),
    })
    await waitFor(() => expect(result.current.roles).toEqual(['FINANCE']))
    expect(result.current.canAccessRoute('/billing')).toBe(true)
    expect(result.current.canAccessRoute('/batch')).toBe(false)
  })
})
