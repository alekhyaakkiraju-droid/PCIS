import type { Meta, StoryObj } from '@storybook/react'
import { MoneyDisplay } from './MoneyDisplay'

const meta: Meta<typeof MoneyDisplay> = {
  title: 'UI/MoneyDisplay',
  component: MoneyDisplay,
  tags: ['autodocs'],
  args: { value: 12500.5 },
}

export default meta
type Story = StoryObj<typeof MoneyDisplay>

export const Default: Story = {}
export const Zero: Story = { args: { value: 0 } }
export const NullValue: Story = { args: { value: null } }
export const StringValue: Story = { args: { value: '980.1' } }
