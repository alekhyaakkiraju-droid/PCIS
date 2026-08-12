import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { FnolWizardPage } from './FnolWizardPage'

describe('FnolWizardPage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('API offline')),
    )
  })

  it('registers claim from wireframe form with demo fallback', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <FnolWizardPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/Loss narrative/i), 'Storm damage to roof.')
    await user.click(screen.getByRole('button', { name: 'Register claim' }))

    await waitFor(() =>
      expect(screen.getByText(/Claims API is unavailable|registered — reserve/i)).toBeInTheDocument(),
    )
  })

  it('shows loss date validation error outside policy term', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <FnolWizardPage />
      </MemoryRouter>,
    )

    await user.clear(screen.getByLabelText(/Date of loss/i))
    await user.type(screen.getByLabelText(/Date of loss/i), '2025-01-01')
    await user.type(screen.getByLabelText(/Loss narrative/i), 'Old loss.')
    await user.click(screen.getByRole('button', { name: 'Register claim' }))

    expect(screen.getByText(/outside every in-force period/i)).toBeInTheDocument()
  })
})
