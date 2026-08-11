import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import App from './App'
import { ErrorBoundary } from './components/ErrorBoundary'
import { authFixtures } from './test-fixtures/authSessions'

vi.mock('./telemetry', () => ({
  initTelemetry: vi.fn(),
}))

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(() => ({
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    removeUser: vi.fn(),
  })),
  WebStorageStateStore: vi.fn(),
}))

describe('App scaffold', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(authFixtures.adjuster), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
  })

  it('renders the router shell without throwing', async () => {
    render(
      <ErrorBoundary>
        <App />
      </ErrorBoundary>,
    )

    expect(await screen.findByRole('navigation', { name: 'Primary' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: /Good morning/i })).toBeInTheDocument()
  })

  it('mounts ErrorBoundary around the application tree', async () => {
    const { container } = render(
      <ErrorBoundary>
        <App />
      </ErrorBoundary>,
    )
    await waitFor(() => expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument())
    expect(container.firstChild).toBeTruthy()
  })
})
