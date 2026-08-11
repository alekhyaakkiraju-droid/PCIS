import { render, screen } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Badge } from './Badge'

describe('Badge', () => {
  it('renders insurance status labels', () => {
    render(<Badge status="Overdue" />)
    expect(screen.getByText('Overdue')).toBeInTheDocument()
  })

  it('has no a11y violations', async () => {
    const { container } = render(
      <>
        <Badge status="Open" />
        <Badge status="Active" />
        <Badge status="Paid" />
      </>,
    )
    expect(await axe(container)).toHaveNoViolations()
  })
})
