import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AppLayout } from './AppLayout'
import { authFixtures } from '../test-fixtures/authSessions'
import { DemoRoleProvider } from '../demo/demo-role'
import { ThemeProvider } from '../theme/ThemeProvider'
import { MENUMD1_ERROR_91 } from '../auth/errors'

const mockUseAuth = vi.fn()

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}))

function ClaimsPage() {
  return <p>Claim inquiry content</p>
}

describe('AuthorizedOutlet', () => {
  it('shows inline 403 for CSR on claims while keeping shell', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.csr.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <ThemeProvider>
      <DemoRoleProvider>
        <MemoryRouter initialEntries={['/claims']}>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/claims" element={<ClaimsPage />} />
            </Route>
          </Routes>
        </MemoryRouter>
      </DemoRoleProvider>
      </ThemeProvider>,
    )

    expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Claim Inquiry' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText(/403 — Access denied/)).toBeInTheDocument()
    expect(screen.getByText(/MENUMD1-91/)).toBeInTheDocument()
    expect(screen.getByText(MENUMD1_ERROR_91.message)).toBeInTheDocument()
    expect(screen.queryByText('Claim inquiry content')).not.toBeInTheDocument()
  })

  it('renders route content for allowed roles', () => {
    mockUseAuth.mockReturnValue({
      status: 'authenticated',
      user: authFixtures.adjuster.user,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <ThemeProvider>
      <DemoRoleProvider>
        <MemoryRouter initialEntries={['/claims']}>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/claims" element={<ClaimsPage />} />
            </Route>
          </Routes>
        </MemoryRouter>
      </DemoRoleProvider>
      </ThemeProvider>,
    )

    expect(screen.getByText('Claim inquiry content')).toBeInTheDocument()
  })
})
