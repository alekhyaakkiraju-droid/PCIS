import type { Meta, StoryObj } from '@storybook/react'
import { Button } from './Button'
import { Card } from './Card'

const meta: Meta<typeof Card> = {
  title: 'UI/Card',
  component: Card,
  tags: ['autodocs'],
  args: {
    header: 'Customer summary',
    children: 'Jordan Lee — Active homeowner policy POL-3001',
    footer: <Button size="sm">Open 360</Button>,
  },
}

export default meta
type Story = StoryObj<typeof Card>

export const Default: Story = {}
export const Loading: Story = { args: { loading: true } }
export const Interactive: Story = {
  args: { interactive: true, footer: undefined, onClick: () => undefined },
}
