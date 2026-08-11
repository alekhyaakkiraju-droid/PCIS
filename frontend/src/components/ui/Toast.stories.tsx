import type { Meta, StoryObj } from '@storybook/react'
import { Button } from './Button'
import { ToastProvider, useToast } from './Toast'

function ToastDemo() {
  const { push } = useToast()
  return (
    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
      <Button onClick={() => push({ message: 'Policy saved', tone: 'success' })}>Success</Button>
      <Button variant="danger" onClick={() => push({ message: 'Payment failed', tone: 'error' })}>
        Error
      </Button>
      <Button variant="secondary" onClick={() => push({ message: 'Renewal due soon', tone: 'warning' })}>
        Warning
      </Button>
      <Button variant="ghost" onClick={() => push({ message: 'Sync complete', tone: 'info' })}>
        Info
      </Button>
    </div>
  )
}

const meta: Meta = {
  title: 'UI/Toast',
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <ToastProvider>
        <Story />
      </ToastProvider>
    ),
  ],
  render: () => <ToastDemo />,
}

export default meta
type Story = StoryObj

export const Default: Story = {}
