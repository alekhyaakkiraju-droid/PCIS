import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AuthProvider } from '@/auth/AuthContext'
import profileFixture from '../../../fixtures/customer-360/profile.json'
import { Customer360Page } from './Customer360Page'

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
    roles: ['CSR'],
  },
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <MemoryRouter>
          <Customer360Page customerId={100001} />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('Customer360Page', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((input: RequestInfo) => {
        const url = typeof input === 'string' ? input : input.url
        if (url.includes('/api/auth/session')) {
          return Promise.resolve(
            new Response(JSON.stringify(csrSession), {
              status: 200,
              headers: { 'Content-Type': 'application/json' },
            }),
          )
        }
        if (url.includes('/v1/customers/100001/360')) {
          return Promise.reject(new Error('offline'))
        }
        if (url.includes('/v1/customers/100001')) {
          return Promise.resolve(
            new Response(JSON.stringify(profileFixture), {
              status: 200,
              headers: { 'Content-Type': 'application/json' },
            }),
          )
        }
        return Promise.reject(new Error(`unexpected ${url}`))
      }),
    )
  })

  it('renders six tabs with independent content', async () => {
    renderPage()
    expect(screen.getByRole('heading', { name: 'Customer 360' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Overview' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Profile' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Policies' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Billing' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Claims' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Audit' })).toBeInTheDocument()
  })

  it('masks tax ID on profile tab', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('tab', { name: 'Profile' }))
    await waitFor(() => expect(screen.getByText('***-**-6789')).toBeInTheDocument())
  })
})
