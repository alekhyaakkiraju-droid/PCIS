import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it, vi } from 'vitest'
import { Button } from './Button'

describe('Button', () => {
  it('renders variants and fires click', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(
      <Button variant="primary" onClick={onClick}>
        Save
      </Button>,
    )
    const button = screen.getByRole('button', { name: 'Save' })
    await user.click(button)
    expect(onClick).toHaveBeenCalledOnce()
  })

  it('supports keyboard activation and loading state', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    const { rerender } = render(<Button onClick={onClick}>Submit</Button>)
    const button = screen.getByRole('button', { name: 'Submit' })
    button.focus()
    await user.keyboard('{Enter}')
    expect(onClick).toHaveBeenCalled()

    rerender(
      <Button loading onClick={onClick}>
        Submit
      </Button>,
    )
    expect(screen.getByRole('button')).toBeDisabled()
    expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true')
  })

  it('has no a11y violations', async () => {
    const { container } = render(<Button>Accessible</Button>)
    expect(await axe(container)).toHaveNoViolations()
  })
})
