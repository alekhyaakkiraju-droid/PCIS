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
  it('renders only nav items allowed for adjuster roles', () => {
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

    expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Claims' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Billing' })).not.toBeInTheDocument()
    expect(screen.getByText('Alice Adjuster')).toBeInTheDocument()
  })

  it('hides claims but shows customers and billing for CSR', () => {
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

    expect(screen.getByRole('link', { name: 'Customers' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Billing' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Claims' })).not.toBeInTheDocument()
  })
})
