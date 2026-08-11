import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it, vi } from 'vitest'
import { Card } from './Card'

describe('Card', () => {
  it('renders header and body', () => {
    render(<Card header="Summary">Body copy</Card>)
    expect(screen.getByText('Summary')).toBeInTheDocument()
    expect(screen.getByText('Body copy')).toBeInTheDocument()
  })

  it('supports interactive keyboard activation', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(
      <Card interactive onClick={onClick} header="Open">
        Details
      </Card>,
    )
    const card = screen.getByRole('button')
    card.focus()
    await user.keyboard('{Enter}')
    expect(onClick).toHaveBeenCalled()
  })

  it('shows skeleton when loading', () => {
    render(
      <Card loading header="Loading">
        Hidden
      </Card>,
    )
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument()
    expect(screen.queryByText('Hidden')).not.toBeInTheDocument()
  })

  it('has no a11y violations', async () => {
    const { container } = render(<Card header="Card">Content</Card>)
    expect(await axe(container)).toHaveNoViolations()
  })
})
