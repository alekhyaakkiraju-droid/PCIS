import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { Sidebar } from './Sidebar'
import { authFixtures } from '../test-fixtures/authSessions'
import { DemoRoleProvider } from '../demo/demo-role'
import { ThemeProvider } from '../theme/ThemeProvider'

const mockUseAuth = vi.fn()

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}))

function renderSidebar() {
  return render(
    <ThemeProvider>
    <DemoRoleProvider>
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    </DemoRoleProvider>
    </ThemeProvider>,
  )
}

describe('Sidebar', () => {
  it('renders enabled claim links for adjuster', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.adjuster.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    renderSidebar()

    expect(screen.getByRole('link', { name: 'Claim Inquiry' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Billing Reconciliation' })).toHaveAttribute('aria-disabled', 'true')
  })

  it('shows disabled claim link for CSR session', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.csr.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    renderSidebar()

    expect(screen.getByRole('link', { name: 'Customer 360' })).toBeInTheDocument()
    const claimLink = screen.getByRole('link', { name: 'Claim Inquiry' })
    expect(claimLink).toHaveAttribute('aria-disabled', 'true')
  })

  it('enables billing for finance role', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.finance.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    renderSidebar()

    expect(screen.getByRole('link', { name: 'Billing Reconciliation' })).not.toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByRole('link', { name: 'Batch Operations' })).toHaveAttribute('aria-disabled', 'true')
  })
})
