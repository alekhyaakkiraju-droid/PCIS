import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AuthProvider } from '@/auth/AuthContext'
import { BillingDashboard } from './BillingDashboard'

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(() => ({
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  })),
  WebStorageStateStore: vi.fn(),
}))

const financeSession = {
  authenticated: true,
  user: {
    sub: 'finance-frank',
    name: 'Frank Finance',
    email: 'frank.finance@pcis.example.com',
    roles: ['FINANCE'],
  },
}

describe('BillingDashboard', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((input: RequestInfo) => {
        const url = typeof input === 'string' ? input : input.url
        if (url.includes('/api/auth/session')) {
          return Promise.resolve(
            new Response(JSON.stringify(financeSession), {
              status: 200,
              headers: { 'Content-Type': 'application/json' },
            }),
          )
        }
        return Promise.reject(new Error('billing API unavailable'))
      }),
    )
  })

  it('renders installment and aging tabs from fixtures', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <AuthProvider>
          <MemoryRouter>
            <BillingDashboard />
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )

    expect(screen.getByRole('heading', { name: 'Billing Dashboard' })).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('table', { name: 'Billing installments' })).toBeInTheDocument(),
    )
    expect(screen.getByText('INST-001')).toBeInTheDocument()
  })
})
