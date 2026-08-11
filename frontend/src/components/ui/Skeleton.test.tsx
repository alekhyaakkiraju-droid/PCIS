import { render, screen } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Skeleton } from './Skeleton'

describe('Skeleton', () => {
  it('announces loading status', () => {
    render(<Skeleton variant="text" lines={2} />)
    expect(screen.getByRole('status', { name: 'Loading' })).toHaveAttribute('aria-busy', 'true')
  })

  it('has no a11y violations', async () => {
    const { container } = render(<Skeleton variant="rectangle" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
