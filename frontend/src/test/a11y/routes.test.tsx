import { render } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/auth/AuthContext'
import { BillingDashboard } from '@/pages/billing/BillingDashboard'
import { Customer360Page } from '@/pages/customers/Customer360Page'
import { FnolWizardPage } from '@/pages/claims/FnolWizardPage'

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(() => ({
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  })),
  WebStorageStateStore: vi.fn(),
}))

const session = {
  authenticated: true,
  user: {
    sub: 'test-user',
    name: 'Test User',
    email: 'test@pcis.example.com',
    roles: ['CSR', 'FINANCE', 'CLAIMS_ADJUSTER', 'UNDERWRITER'],
  },
}

function renderRoute(element: React.ReactElement, path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path={path} element={element} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('route-level accessibility', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((input: RequestInfo) => {
        const url = typeof input === 'string' ? input : input.url
        if (url.includes('/api/auth/session')) {
          return Promise.resolve(
            new Response(JSON.stringify(session), {
              status: 200,
              headers: { 'Content-Type': 'application/json' },
            }),
          )
        }
        return Promise.reject(new Error('offline'))
      }),
    )
  })

  it('Customer 360 route has no serious axe violations', async () => {
    const { container } = renderRoute(<Customer360Page customerId={100001} />, '/customers/100001')
    expect(await axe(container)).toHaveNoViolations()
  })

  it('FNOL route has no serious axe violations', async () => {
    const { container } = renderRoute(<FnolWizardPage />, '/claims/fnol')
    expect(await axe(container)).toHaveNoViolations()
  })

  it('Billing route has no serious axe violations', async () => {
    const { container } = renderRoute(<BillingDashboard />, '/billing')
    expect(await axe(container)).toHaveNoViolations()
  })
})
