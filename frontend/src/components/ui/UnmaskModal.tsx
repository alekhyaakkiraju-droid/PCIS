import { useState } from 'react'
import { Button } from './Button'
import { Input } from './Input'
import { Modal } from './Modal'

export type UnmaskModalProps = {
  open: boolean
  fieldLabel?: string
  onClose: () => void
  onConfirm: (justification: string) => void
}

export function UnmaskModal({
  open,
  fieldLabel = 'tax ID',
  onClose,
  onConfirm,
}: UnmaskModalProps) {
  const [reason, setReason] = useState('')

  const confirmDisabled = reason.trim().length < 6

  const handleClose = () => {
    setReason('')
    onClose()
  }

  const handleConfirm = () => {
    if (confirmDisabled) return
    onConfirm(reason.trim())
    setReason('')
  }

  return (
    <Modal
      open={open}
      title="Unmask restricted value"
      onClose={handleClose}
      footer={
        <>
          <Button variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button variant="primary" disabled={confirmDisabled} onClick={handleConfirm}>
            View unmasked
          </Button>
        </>
      }
    >
      <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginBottom: 'var(--pcis-space-4)' }}>
        Viewing the full {fieldLabel} requires a recorded justification. This action is itself audited.
      </p>
      <Input
        label="Justification"
        name="unmaskReason"
        placeholder="e.g. fraud investigation case FI-2026-0042"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
    </Modal>
  )
}
