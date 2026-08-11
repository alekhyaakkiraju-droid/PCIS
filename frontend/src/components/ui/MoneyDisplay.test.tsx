import { render, screen } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { formatMoney, MoneyDisplay } from './MoneyDisplay'

describe('MoneyDisplay', () => {
  it('formats with two decimal places', () => {
    expect(formatMoney(12500.5)).toBe('$12,500.50')
    render(<MoneyDisplay value={980.1} />)
    expect(screen.getByText('$980.10')).toBeInTheDocument()
  })

  it('renders dash for null/undefined/NaN', () => {
    expect(formatMoney(null)).toBe('—')
    expect(formatMoney(undefined)).toBe('—')
    expect(formatMoney(Number.NaN)).toBe('—')
    render(<MoneyDisplay value={undefined} />)
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('has no a11y violations', async () => {
    const { container } = render(<MoneyDisplay value={42} />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
