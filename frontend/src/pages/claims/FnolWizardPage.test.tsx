import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AuthProvider } from '@/auth/AuthContext'
import { FnolWizardPage } from './FnolWizardPage'

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(() => ({
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  })),
  WebStorageStateStore: vi.fn(),
}))

const adjusterSession = {
  authenticated: true,
  user: {
    sub: 'adjuster-alice',
    name: 'Alice Adjuster',
    email: 'alice.adjuster@pcis.example.com',
    roles: ['CLAIMS_ADJUSTER'],
    authority_limit: 25000,
  },
}

function renderWizard() {
  const client = new QueryClient()
  return render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <MemoryRouter>
          <FnolWizardPage />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('FnolWizardPage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((input: RequestInfo) => {
        const url = typeof input === 'string' ? input : input.url
        if (url.includes('/api/auth/session')) {
          return Promise.resolve(
            new Response(JSON.stringify(adjusterSession), {
              status: 200,
              headers: { 'Content-Type': 'application/json' },
            }),
          )
        }
        if (url.includes('/v1/claims') && !url.includes('/payments')) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                claimNbr: 'CLM000000099',
                polNbr: 'POL-3001',
                custId: 100001,
                lossDate: '2026-03-01',
                claimType: 'PRP',
                claimStatus: 'O',
              }),
              { status: 201, headers: { 'Content-Type': 'application/json' } },
            ),
          )
        }
        return Promise.reject(new Error(`unexpected ${url}`))
      }),
    )
  })

  it('walks through FNOL steps and submits', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(globalThis.fetch)
    renderWizard()

    await user.type(screen.getByLabelText(/Policy number/i), 'POL-3001')
    await user.type(screen.getByLabelText(/Customer ID/i), '100001')
    await user.type(screen.getByLabelText(/Loss date/i), '2026-03-01')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    await user.type(screen.getByLabelText(/Description/i), 'Storm damage to roof.')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(screen.getByText('Storm damage to roof.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Submit FNOL' }))

    await waitFor(() => {
      const claimPosts = fetchMock.mock.calls.filter(([input, init]) => {
        const url = typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
        return url.includes('/v1/claims') && (init?.method === 'POST' || (input instanceof Request && input.method === 'POST'))
      })
      expect(claimPosts.length).toBeGreaterThan(0)
    })
  })
})
