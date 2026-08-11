import type { Meta, StoryObj } from '@storybook/react'
import { Skeleton } from './Skeleton'

const meta: Meta<typeof Skeleton> = {
  title: 'UI/Skeleton',
  component: Skeleton,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof Skeleton>

export const Rectangle: Story = { args: { variant: 'rectangle', height: 48 } }
export const Circle: Story = { args: { variant: 'circle' } }
export const TextLines: Story = { args: { variant: 'text', lines: 4 } }
