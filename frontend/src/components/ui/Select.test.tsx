import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Select } from './Select'

const options = [
  { value: 'auto', label: 'Auto' },
  { value: 'home', label: 'Homeowners' },
]

describe('Select', () => {
  it('renders options and changes value', async () => {
    const user = userEvent.setup()
    render(<Select label="Product" options={options} defaultValue="auto" />)
    const select = screen.getByLabelText('Product')
    await user.selectOptions(select, 'home')
    expect(select).toHaveValue('home')
  })

  it('shows error messaging', () => {
    render(<Select label="Product" options={options} errorMessage="Required" />)
    expect(screen.getByRole('alert')).toHaveTextContent('Required')
    expect(screen.getByLabelText('Product')).toHaveAttribute('aria-invalid', 'true')
  })

  it('has no a11y violations', async () => {
    const { container } = render(<Select label="Product" options={options} />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
