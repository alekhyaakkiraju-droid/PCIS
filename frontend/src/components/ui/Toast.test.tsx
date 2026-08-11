import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Button } from './Button'
import { ToastProvider, useToast } from './Toast'

function Probe() {
  const { push } = useToast()
  return <Button onClick={() => push({ message: 'Saved', tone: 'success', durationMs: 0 })}>Notify</Button>
}

describe('Toast', () => {
  it('announces toasts in a live region and supports dismiss', async () => {
    const user = userEvent.setup()
    render(
      <ToastProvider>
        <Probe />
      </ToastProvider>,
    )
    await user.click(screen.getByRole('button', { name: 'Notify' }))
    expect(screen.getByRole('status')).toHaveTextContent('Saved')
    await user.click(screen.getByRole('button', { name: 'Dismiss notification' }))
    expect(screen.queryByText('Saved')).not.toBeInTheDocument()
  })

  it('has no a11y violations', async () => {
    const { container } = render(
      <ToastProvider>
        <Probe />
      </ToastProvider>,
    )
    expect(await axe(container)).toHaveNoViolations()
  })
})
