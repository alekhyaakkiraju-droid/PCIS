import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { Sidebar } from './Sidebar'
import { authFixtures } from '../test-fixtures/authSessions'

const mockUseAuth = vi.fn()

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}))

describe('Sidebar', () => {
  it('renders all wireframe nav links for authenticated users', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.adjuster.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Claim Inquiry' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Billing Reconciliation' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Admin & Compliance' })).toBeInTheDocument()
  })

  it('shows customer link for CSR session', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.csr.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Customer 360' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Claim Inquiry' })).toBeInTheDocument()
  })
})
