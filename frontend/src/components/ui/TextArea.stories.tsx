import type { Meta, StoryObj } from '@storybook/react'
import { TextArea } from './TextArea'

const meta: Meta<typeof TextArea> = {
  title: 'UI/TextArea',
  component: TextArea,
  tags: ['autodocs'],
  args: { label: 'Loss description', placeholder: 'Describe what happened…' },
}

export default meta
type Story = StoryObj<typeof TextArea>

export const Default: Story = {}
export const WithCount: Story = {
  args: { showCount: true, maxLength: 200, defaultValue: 'Vehicle struck a parked car.' },
}
export const ErrorState: Story = {
  args: { errorMessage: 'Description is required for FNOL' },
}
