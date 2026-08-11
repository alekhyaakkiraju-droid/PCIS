import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { TextArea } from './TextArea'

describe('TextArea', () => {
  it('updates value and shows character count', async () => {
    const user = userEvent.setup()
    render(<TextArea label="Notes" showCount maxLength={20} />)
    const area = screen.getByLabelText('Notes')
    await user.type(area, 'Hello')
    expect(area).toHaveValue('Hello')
    expect(screen.getByText('5/20')).toBeInTheDocument()
  })

  it('has no a11y violations', async () => {
    const { container } = render(<TextArea label="Loss description" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
