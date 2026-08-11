import type { Meta, StoryObj } from '@storybook/react'
import { Badge } from './Badge'

const meta: Meta<typeof Badge> = {
  title: 'UI/Badge',
  component: Badge,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof Badge>

export const ClaimOpen: Story = { args: { status: 'Open' } }
export const ClaimApproved: Story = { args: { status: 'Approved' } }
export const ClaimDenied: Story = { args: { status: 'Denied' } }
export const PolicyActive: Story = { args: { status: 'Active' } }
export const PolicyRenewal: Story = { args: { status: 'Renewal' } }
export const BillingOverdue: Story = { args: { status: 'Overdue' } }
export const BillingPaid: Story = { args: { status: 'Paid' } }
