import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Input } from './Input'

describe('Input', () => {
  it('associates label and accepts typed value', async () => {
    const user = userEvent.setup()
    render(<Input label="Policy number" />)
    const input = screen.getByLabelText('Policy number')
    await user.type(input, 'POL-3001')
    expect(input).toHaveValue('POL-3001')
  })

  it('exposes error message via aria-describedby', () => {
    render(<Input label="Email" errorMessage="Invalid email" />)
    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Invalid email')
  })

  it('has no a11y violations', async () => {
    const { container } = render(<Input label="Customer name" hint="Legal name" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
