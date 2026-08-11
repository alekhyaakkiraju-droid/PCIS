import type { Meta, StoryObj } from '@storybook/react'
import { Input } from './Input'

const meta: Meta<typeof Input> = {
  title: 'UI/Input',
  component: Input,
  tags: ['autodocs'],
  args: { label: 'Policy number', placeholder: 'POL-3001' },
}

export default meta
type Story = StoryObj<typeof Input>

export const Default: Story = {}
export const Required: Story = { args: { required: true } }
export const WithHint: Story = { args: { hint: 'Enter the 8-character policy id' } }
export const ErrorState: Story = {
  args: { validationState: 'error', errorMessage: 'Policy number is required' },
}
export const SuccessState: Story = {
  args: { validationState: 'success', hint: 'Looks good' },
}
export const DateType: Story = { args: { type: 'date', label: 'Effective date' } }
export const NumberType: Story = { args: { type: 'number', label: 'Deductible' } }
