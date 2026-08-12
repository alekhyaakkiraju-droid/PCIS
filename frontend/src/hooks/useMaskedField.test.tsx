import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '@/auth/AuthContext'
import { DemoRoleProvider } from '@/demo/demo-role'
import { maskPiiValue, useMaskedField } from './useMaskedField'

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(() => ({
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  })),
  WebStorageStateStore: vi.fn(),
}))

const csrSession = {
  authenticated: true,
  user: {
    sub: 'csr-carol',
    name: 'Carol CSR',
    email: 'carol.csr@pcis.example.com',
    roles: ['CSR'] as const,
  },
}

const complianceSession = {
  authenticated: true,
  user: {
    sub: 'compliance-pat',
    name: 'Pat Compliance',
    email: 'pat.compliance@pcis.example.com',
    roles: ['COMPLIANCE'] as const,
  },
}

function wrapper(session: { authenticated: boolean; user: { sub: string; name: string; email: string; roles: readonly string[] } }) {
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

describe('maskPiiValue', () => {
  it('masks tax ID to last four digits', () => {
    expect(maskPiiValue('123-45-6789', 'taxId')).toBe('***-**-6789')
  })

  it('masks phone numbers', () => {
    expect(maskPiiValue('217-555-0101', 'phone')).toBe('***-***-0101')
  })

  it('masks email addresses', () => {
    expect(maskPiiValue('alice@example.com', 'email')).toBe('a***@example.com')
  })
})

describe('useMaskedField', () => {
  it('masks values for non-compliance roles', async () => {
    const { result } = renderHook(() => useMaskedField('123-45-6789', 'taxId'), {
      wrapper: wrapper(csrSession),
    })
    await waitFor(() => expect(result.current.displayValue).toBe('***-**-6789'))
    expect(result.current.canToggle).toBe(true)
    act(() => result.current.toggleReveal())
    expect(result.current.displayValue).toBe('123-45-6789')
  })

  it('shows unmasked values for compliance role', async () => {
    const { result } = renderHook(() => useMaskedField('123-45-6789', 'taxId'), {
      wrapper: wrapper(complianceSession),
    })
    await waitFor(() => expect(result.current.displayValue).toBe('123-45-6789'))
    expect(result.current.isMasked).toBe(false)
    expect(result.current.canToggle).toBe(false)
  })
})
