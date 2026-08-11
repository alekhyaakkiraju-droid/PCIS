import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import { ErrorBoundary } from './components/ErrorBoundary'

vi.mock('./telemetry', () => ({
  initTelemetry: vi.fn(),
}))

describe('App scaffold', () => {
  it('renders the router shell without throwing', async () => {
    render(
      <ErrorBoundary>
        <App />
      </ErrorBoundary>,
    )

    expect(await screen.findByRole('navigation', { name: 'Primary' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
  })

  it('mounts ErrorBoundary around the application tree', () => {
    const { container } = render(
      <ErrorBoundary>
        <App />
      </ErrorBoundary>,
    )
    expect(container.firstChild).toBeTruthy()
  })
})
