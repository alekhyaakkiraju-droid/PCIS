import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { BillingDashboard } from './BillingDashboard'

describe('BillingDashboard', () => {
  it('renders wireframe installment reconciliation table', () => {
    render(
      <MemoryRouter>
        <BillingDashboard />
      </MemoryRouter>,
    )

    expect(screen.getByRole('tablist', { name: 'Billing views' })).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Billing reconciliation installments' })).toBeInTheDocument()
    expect(screen.getByText('POL-0088217')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Approve cutover gate' })).toBeInTheDocument()
  })
})
