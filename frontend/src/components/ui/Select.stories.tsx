import type { Meta, StoryObj } from '@storybook/react'
import { Select } from './Select'

const meta: Meta<typeof Select> = {
  title: 'UI/Select',
  component: Select,
  tags: ['autodocs'],
  args: {
    label: 'Line of business',
    placeholder: 'Select a product',
    options: [
      { value: 'auto', label: 'Auto' },
      { value: 'home', label: 'Homeowners' },
      { value: 'renters', label: 'Renters' },
    ],
  },
}

export default meta
type Story = StoryObj<typeof Select>

export const Default: Story = {}
export const WithGroups: Story = {
  args: {
    options: undefined,
    optionGroups: [
      {
        label: 'Personal',
        options: [
          { value: 'auto', label: 'Auto' },
          { value: 'home', label: 'Homeowners' },
        ],
      },
      {
        label: 'Commercial',
        options: [{ value: 'commercial', label: 'Commercial package' }],
      },
    ],
  },
}
export const ErrorState: Story = {
  args: { errorMessage: 'Select a product to continue' },
}
export const Disabled: Story = { args: { disabled: true } }
