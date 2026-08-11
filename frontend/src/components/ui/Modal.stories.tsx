import { useState } from 'react'
import type { Meta, StoryObj } from '@storybook/react'
import { Button } from './Button'
import { Modal } from './Modal'

function ModalDemo() {
  const [open, setOpen] = useState(true)
  return (
    <>
      <Button onClick={() => setOpen(true)}>Open modal</Button>
      <Modal
        open={open}
        title="Confirm claim settlement"
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={() => setOpen(false)}>Confirm</Button>
          </>
        }
      >
        <p>Settle claim CLM-5004 for $4,500.25?</p>
      </Modal>
    </>
  )
}

const meta: Meta<typeof Modal> = {
  title: 'UI/Modal',
  component: Modal,
  tags: ['autodocs'],
  render: () => <ModalDemo />,
}

export default meta
type Story = StoryObj<typeof Modal>

export const Default: Story = {}
