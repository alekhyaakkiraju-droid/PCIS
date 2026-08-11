import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Tabs } from './Tabs'

const items = [
  { id: 'one', label: 'Overview', content: <p>Overview panel</p> },
  { id: 'two', label: 'Policies', content: <p>Policies panel</p> },
  { id: 'three', label: 'Claims', content: <p>Claims panel</p>, disabled: true },
]

describe('Tabs', () => {
  it('switches panels and supports arrow keys', async () => {
    const user = userEvent.setup()
    render(<Tabs items={items} />)
    expect(screen.getByText('Overview panel')).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Policies' }))
    expect(screen.getByText('Policies panel')).toBeInTheDocument()

    const policies = screen.getByRole('tab', { name: 'Policies' })
    policies.focus()
    await user.keyboard('{ArrowLeft}')
    expect(screen.getByRole('tab', { name: 'Overview' })).toHaveFocus()
    expect(screen.getByText('Overview panel')).toBeInTheDocument()
  })

  it('has no a11y violations', async () => {
    const { container } = render(<Tabs items={items} />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
