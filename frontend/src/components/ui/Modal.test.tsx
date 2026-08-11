import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it, vi } from 'vitest'
import { Modal } from './Modal'

describe('Modal', () => {
  it('renders dialog attributes and closes on Escape', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <Modal open title="Confirm" onClose={onClose}>
        <p>Are you sure?</p>
      </Modal>,
    )
    expect(screen.getByRole('dialog', { name: 'Confirm' })).toHaveAttribute('aria-modal', 'true')
    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalled()
  })

  it('does not render when closed', () => {
    const { container } = render(
      <Modal open={false} title="Hidden" onClose={() => undefined}>
        Nope
      </Modal>,
    )
    expect(container).toBeEmptyDOMElement()
  })

  it('has no a11y violations when open', async () => {
    const { container } = render(
      <Modal open title="Accessible modal" onClose={() => undefined}>
        <p>Body</p>
      </Modal>,
    )
    expect(await axe(container)).toHaveNoViolations()
  })
})
