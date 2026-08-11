import { useEffect, useId, useRef, type ReactNode } from 'react'
import FocusTrap from 'focus-trap-react'
import { Button } from './Button'
import styles from './Modal.module.css'

export interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  closeOnBackdrop?: boolean
  initialFocusRef?: React.RefObject<HTMLElement | null>
}

export function Modal({
  open,
  title,
  onClose,
  children,
  footer,
  closeOnBackdrop = true,
  initialFocusRef,
}: ModalProps) {
  const titleId = useId()
  const closeRef = useRef<HTMLButtonElement>(null)
  const previouslyFocused = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!open) return
    previouslyFocused.current = document.activeElement as HTMLElement | null
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      previouslyFocused.current?.focus?.()
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className={styles.backdrop}
      onMouseDown={(event) => {
        if (closeOnBackdrop && event.target === event.currentTarget) onClose()
      }}
    >
      <FocusTrap
        focusTrapOptions={{
          initialFocus: () =>
            initialFocusRef?.current ?? closeRef.current ?? undefined,
          allowOutsideClick: true,
          fallbackFocus: () => closeRef.current ?? document.body,
        }}
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby={titleId}
          className={styles.dialog}
        >
          <div className={styles.header}>
            <h2 id={titleId} className={styles.title}>
              {title}
            </h2>
            <button
              ref={closeRef}
              type="button"
              className={styles.close}
              aria-label="Close dialog"
              onClick={onClose}
            >
              ×
            </button>
          </div>
          <div>{children}</div>
          {footer ? <div className={styles.footer}>{footer}</div> : null}
          {!footer ? (
            <div className={styles.footer}>
              <Button variant="secondary" onClick={onClose}>
                Close
              </Button>
            </div>
          ) : null}
        </div>
      </FocusTrap>
    </div>
  )
}
