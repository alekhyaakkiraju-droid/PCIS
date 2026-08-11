import type { Meta, StoryObj } from '@storybook/react'
import { Tabs } from './Tabs'

const meta: Meta<typeof Tabs> = {
  title: 'UI/Tabs',
  component: Tabs,
  tags: ['autodocs'],
  args: {
    items: [
      { id: 'overview', label: 'Overview', content: <p>Customer overview content</p> },
      { id: 'policies', label: 'Policies', content: <p>Policy list content</p> },
      { id: 'claims', label: 'Claims', content: <p>Claims list content</p> },
      { id: 'billing', label: 'Billing', content: <p>Billing content</p>, disabled: true },
    ],
  },
}

export default meta
type Story = StoryObj<typeof Tabs>

export const Default: Story = {}
export const PoliciesDefault: Story = { args: { defaultTabId: 'policies' } }
